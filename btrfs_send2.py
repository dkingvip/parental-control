import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

# Stop Docker first
print('=== Stop Docker ===')
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S /usr/syno/bin/synopkg stop Docker')
time.sleep(10)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
print(re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace')))

# Check what's on volume2
print('\n=== Check volume2 state ===')
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "ls -la /volume2/ | grep docker; du -sh /volume2/@docker* 2>/dev/null"')
time.sleep(5)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
print(re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace')))

# Try btrfs send using full subvol path from the btrfs root (device path)
print('\n=== btrfs send test ===')
ch = t.open_session()
# The @docker subvol on md2 (volume1) - use the device mount path approach
# Try sending from /dev/md2 btrfs root
cmd = 'echo 123123 | sudo -S bash -c "btrfs send -p /volume1 /volume1 2>&1 | head -c 100"'
ch.exec_command(cmd)
time.sleep(10)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
print(re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace')))

c.close()

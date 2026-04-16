import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

# Check what @docker is on volume2
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "ls -la /volume2/@docker 2>&1; btrfs subvolume list /volume2/@docker 2>&1 | head -5"')
time.sleep(6)
s = b''
while ch.recv_ready(): s += ch.recv(8192)
ch.close()
txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))
print('volume2/@docker:', txt)

# Try a simple btrfs send/receive test to /tmp
ch2 = t.open_session()
ch2.exec_command('echo 123123 | sudo -S bash -c "btrfs send /volume1/@docker 2>&1 | head -c 100"')
time.sleep(10)
s2 = b''
while ch2.recv_ready(): s2 += ch2.recv(4096)
ch2.close()
txt2 = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s2.decode('utf-8', errors='replace'))
print('\nbtrfs send test:', txt2[:500])

c.close()

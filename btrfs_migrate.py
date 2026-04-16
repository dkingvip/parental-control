import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

# Stop Docker
print('=== Stop Docker ===')
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S /usr/syno/bin/synopkg stop Docker')
time.sleep(10)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
print(re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace')))

# Rename @docker_new to @docker_v1
print('\n=== Rename @docker_new -> @docker_v1 ===')
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S mv /volume2/@docker_new /volume2/@docker_v1')
time.sleep(5)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
print(re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace')))

# Now do btrfs send/receive from volume1 to volume2
# This will create a NEW @docker subvolume on volume2
print('\n=== btrfs send/receive ===')
print('This may take 10-30 minutes for 20GB on degraded disk...')

ch = t.open_session()
# Send @docker from volume1 and receive to volume2
# Use -f to force create if @docker already exists as dir (it shouldn't as a subvolume now)
cmd = 'echo 123123 | sudo -S bash -c "btrfs send /volume1/@docker 2>&1 | btrfs receive /volume2/ 2>&1; echo RECEIVE_EXIT:\$?"'
ch.exec_command(cmd)
time.sleep(5)

# Poll for completion - check every 30s
for i in range(60):
    time.sleep(30)
    ch2 = t.open_session()
    ch2.exec_command('echo 123123 | sudo -S bash -c "ps aux | grep btrfs | grep -v grep | wc -l; tail -3 /tmp/btrfs_recv.log 2>/dev/null"')
    time.sleep(4)
    s2 = b''
    while ch2.recv_ready(): s2 += ch2.recv(4096)
    ch2.close()
    txt2 = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s2.decode('utf-8', errors='replace'))
    lines = [l.strip() for l in txt2.split('\n') if l.strip()]
    print('[%ds]' % ((i+1)*30), ' | '.join([l for l in lines if 'sudo' not in l and 'bash' not in l]))
    if lines and lines[0] == '0':
        print('DONE!')
        break

# Final result
time.sleep(3)
s = b''
while ch.recv_ready(): s += ch.recv(16384)
ch.close()
print('\nbtrfs output:', re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))[-1000:])

c.close()

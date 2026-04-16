import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

# Check @docker_v1 content - is it the old Docker images?
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "du -sh /volume2/@docker_v1; ls /volume2/@docker_v1/; du -sh /volume2/@docker_v1/btrfs 2>/dev/null; du -sh /volume2/@docker_v1/image 2>/dev/null; du -sh /volume2/@docker_v1/volumes 2>/dev/null"')
time.sleep(6)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))
print('@docker_v1 content:', txt)

# Check what was in @docker (old) - how many image subvolumes
ch2 = t.open_session()
ch2.exec_command('echo 123123 | sudo -S bash -c "du -sh /volume1/@docker/btrfs 2>/dev/null; find /volume1/@docker/btrfs/subvolumes -maxdepth 1 -mindepth 1 | wc -l; du -sh /volume1/@docker/image 2>/dev/null"')
time.sleep(6)
s2 = b''
while ch2.recv_ready(): s2 += ch2.recv(4096)
ch2.close()
txt2 = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s2.decode('utf-8', errors='replace'))
print('\n@v1 old docker data:', txt2)

c.close()

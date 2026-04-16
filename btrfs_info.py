import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

# Get subvolume info
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "btrfs subvolume list /volume1/@docker; echo ---; btrfs subvolume list /volume2/@docker 2>/dev/null; echo ---; btrfs subvolume list /volume2/@docker_new 2>/dev/null"')
time.sleep(6)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))
print(txt)

c.close()

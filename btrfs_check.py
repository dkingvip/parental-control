import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "ps aux | grep btrfs | grep -v grep; du -sh /volume2/@docker 2>/dev/null || echo no_vol2_docker; du -sh /volume2/@docker_v1 2>/dev/null || echo no_vol2_docker_v1; du -sh /volume2 2>/dev/null"')
time.sleep(6)
s = b''
while ch.recv_ready(): s += ch.recv(8192)
ch.close()
txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))
print(txt)
c.close()

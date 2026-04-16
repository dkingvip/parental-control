import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "du -sh /volume2/@docker_new; find /volume2/@docker_new -type f | wc -l; echo ---OLD---; du -sh /volume1/@docker 2>/dev/null || echo VOL1_GONE; du -sh /volume1/@docker_broken 2>/dev/null || echo VOL1_BROKEN_GONE; find /volume1/@docker_broken -type f 2>/dev/null | wc -l"')
time.sleep(8)
s = b''
while ch.recv_ready(): s += ch.recv(16384)
ch.close()
txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))
print(txt)

c.close()

import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

for i in range(5):
    time.sleep(30)
    ch = t.open_session()
    ch.exec_command('echo 123123 | sudo -S bash -c "ps aux | grep btrfs | grep -v grep; du -sh /volume2/@docker 2>/dev/null || du -sh /volume2/@docker_v1 2>/dev/null || echo not_created_yet"')
    time.sleep(5)
    s = b''
    while ch.recv_ready(): s += ch.recv(4096)
    ch.close()
    txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))
    lines = [l.strip() for l in txt.split('\n') if l.strip() and 'sudo' not in l and 'bash' not in l]
    print('[%ds]' % ((i+1)*30), ' | '.join(lines))
c.close()

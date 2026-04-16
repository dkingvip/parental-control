import paramiko, sys, time
sys.stdout.reconfigure(encoding='utf-8')
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
transport = c.get_transport()

ch = transport.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "ps aux | grep rsync | grep -v grep; du -sh /volume2/@docker_new 2>/dev/null; du -sh /volume1/@docker 2>/dev/null; tail -3 /tmp/rsync.log 2>/dev/null"')
time.sleep(5)
stdout = b''
while ch.recv_ready():
    stdout += ch.recv(8192)
ch.close()
c.close()
txt = stdout.decode('utf-8', errors='replace')
import re
txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', txt)
print(txt)

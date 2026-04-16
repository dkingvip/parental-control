import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "iostat -x 1 5 2>/dev/null | head -30 || cat /proc/diskstats | head -20"')
time.sleep(8)
s = b''
while ch.recv_ready(): s += ch.recv(16384)
ch.close()
txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))
print(txt)

# Also check disk errors
ch2 = t.open_session()
ch2.exec_command('echo 123123 | sudo -S bash -c "cat /proc/mdstat && echo --- && cat /proc/mdstat && dmesg | grep -i error | tail -10 2>/dev/null"')
time.sleep(5)
s2 = b''
while ch2.recv_ready(): s2 += ch2.recv(8192)
ch2.close()
txt2 = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s2.decode('utf-8', errors='replace'))
print(txt2)
c.close()

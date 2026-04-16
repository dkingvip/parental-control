import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

# Fix DNS - use 8.8.8.8 and 8.8.4.4 via router gateway
print('=== Fix DNS to use Google DNS ===')
ch = t.open_session()
# Add Google DNS to resolv.conf
ch.exec_command('echo 123123 | sudo -S bash -c "cp /etc/resolv.conf /etc/resolv.conf.bak && echo nameserver 8.8.8.8 > /etc/resolv.conf && echo nameserver 8.8.4.4 >> /etc/resolv.conf && cat /etc/resolv.conf"')
time.sleep(5)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
print(re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace')))

# Test DNS now
print('\n=== Test DNS ===')
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "curl -I --connect-timeout 8 https://registry-1.docker.io/v2/ 2>&1 | head -5; nslookup docker.io 8.8.8.8 2>&1 | head -10"')
time.sleep(12)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
print(re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace')))

c.close()

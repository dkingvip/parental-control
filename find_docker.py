import paramiko, sys, time

sys.stdout.reconfigure(encoding='utf-8')
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)

transport = c.get_transport()
channel = transport.open_session()

cmd = 'echo 123123 | sudo -S bash -c "which docker 2>&1; ls /var/packages/Docker/bin/ 2>&1; ls /usr/local/bin/ | grep docker 2>&1; cat /var/packages/Docker/scripts/startup 2>&1 | grep docker | head -5"'
channel.exec_command(cmd)
time.sleep(8)

stdout = b''
while channel.recv_ready():
    stdout += channel.recv(16384)
exit_status = channel.recv_exit_status()

print(stdout.decode('utf-8', errors='replace'))
print('EXIT:', exit_status)
c.close()

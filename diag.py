import paramiko, sys, time

sys.stdout.reconfigure(encoding='utf-8')
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)

transport = c.get_transport()

cmds = [
    # Check what's in the new location
    'echo 123123 | sudo -S bash -c "du -sh /volume2/@docker_new && ls -la /volume2/@docker_new/ && du -sh /volume2/@docker_new/btrfs 2>&1"',
    # Check original location
    'echo 123123 | sudo -S bash -c "du -sh /volume1/@docker_broken 2>/dev/null && du -sh /volume1/@docker_broken/btrfs 2>/dev/null"',
    # Check Docker daemon config (registry mirror)
    'echo 123123 | sudo -S bash -c "cat /var/packages/Docker/etc/dockerd.json"',
    # Check dockerd logs for errors
    'echo 123123 | sudo -S bash -c "cat /var/packages/Docker/target/log/dockerd.log 2>/dev/null | tail -20"',
    # Test Docker Hub connectivity
    'echo 123123 | sudo -S bash -c "curl -I --connect-timeout 10 https://registry-1.docker.io/v2/ 2>&1 | head -10"',
    # Test the mirror they configured
    'echo 123123 | sudo -S bash -c "curl -I --connect-timeout 10 https://docker.rainbond.cc/v2/ 2>&1 | head -10"',
]

for cmd in cmds:
    channel = transport.open_session()
    channel.exec_command(cmd)
    time.sleep(10)
    stdout = b''
    while channel.recv_ready():
        stdout += channel.recv(8192)
    print('CMD:', cmd[:80])
    print('OUT:', stdout.decode('utf-8', errors='replace').strip())
    print()
    channel.close()

c.close()

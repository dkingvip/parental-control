import paramiko, sys, time

sys.stdout.reconfigure(encoding='utf-8')
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)

transport = c.get_transport()

# Check what exists in vol1 broken
cmds = [
    # Check if vol1 is still accessible at all (read-only)
    'echo 123123 | sudo -S bash -c "ls -la /volume1/ 2>&1 | head -10"',
    # Check what's actually in the copied location vs what Docker needs
    'echo 123123 | sudo -S bash -c "ls /volume2/@docker_new/image/ 2>&1"',
    'echo 123123 | sudo -S bash -c "ls /volume2/@docker_new/btrfs/ 2>&1"',
    # Check original btrfs contents 
    'echo 123123 | sudo -S bash -c "ls /volume1/@docker_broken/btrfs/ 2>&1 | head -20"',
    # Disk space on volume2
    'echo 123123 | sudo -S bash -c "df -h /volume2"',
]

for cmd in cmds:
    channel = transport.open_session()
    channel.exec_command(cmd)
    time.sleep(8)
    stdout = b''
    while channel.recv_ready():
        stdout += channel.recv(8192)
    print('CMD:', cmd[:70])
    print('OUT:', stdout.decode('utf-8', errors='replace').strip())
    print()
    channel.close()

c.close()

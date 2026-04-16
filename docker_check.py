import paramiko, time, re, sys
sys.stdout.reconfigure(encoding='utf-8')
host, pw = '192.168.0.128', '123123'

c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect(host, username='admin', password=pw, timeout=10)
transport = c.get_transport()

def cmd(cmd_str, timeout=8):
    channel = transport.open_session()
    # Write password to stdin for sudo
    stdin = channel.makefile('wb')
    stdout = channel.makefile('rb')
    stderr = channel.makefile_stderr('rb')
    stdin.write(pw.encode() + b'\n')
    stdin.flush()
    channel.exec_command(cmd_str)
    time.sleep(timeout)
    out = stdout.read().decode('utf-8', errors='replace')
    err = stderr.read().decode('utf-8', errors='replace')
    stdin.close()
    stdout.close()
    stderr.close()
    channel.close()
    return out, err

# 1. Start Docker
print('=== 1. Start Docker ===')
out, err = cmd('/usr/syno/bin/synopkg start Docker', 20)
print('OUT:', out[:300])
print('ERR:', err[:200])

# 2. Wait for dockerd
print('\n=== 2. Wait for dockerd ===')
for i in range(10):
    time.sleep(5)
    out, err = cmd('ps aux | grep dockerd', 5)
    if 'dockerd' in out:
        print('dockerd is running!')
        break
    print('waiting...', i, out[:50])

# 3. Check Docker info
print('\n=== 3. Docker info ===')
out, err = cmd('/var/packages/Docker/target/usr/bin/docker info 2>&1 | head -10', 10)
print('OUT:', out[:400])
print('ERR:', err[:100])

# 4. Check images
print('\n=== 4. Images ===')
out, err = cmd('/var/packages/Docker/target/usr/bin/docker images', 8)
print('OUT:', out[:400])

# 5. Check HA dir
print('\n=== 5. HA dir ===')
out, err = cmd('ls /volume2/homeassistant/', 5)
print('OUT:', out[:200])

# 6. docker-compose.yml check
print('\n=== 6. Compose file ===')
out, err = cmd('cat /volume2/homeassistant/docker-compose.yml', 5)
print('OUT:', out[:300])

c.close()

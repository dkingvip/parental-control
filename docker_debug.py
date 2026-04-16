import paramiko, time, re, sys
sys.stdout.reconfigure(encoding='utf-8')
host, pw = '192.168.0.128', '123123'

c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect(host, username='admin', password=pw, timeout=10)
sh = c.invoke_shell(width=200, height=80)
time.sleep(2)

def recv(sh, t=4):
    d = b''
    sh.settimeout(t)
    try:
        while True:
            x = sh.recv(4096)
            if not x: break
            d += x
    except: pass
    return re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', d.decode('utf-8', errors='replace'))

def sudo(sh, cmd, wait=5):
    sh.send(cmd + '\n')
    time.sleep(1)
    r = recv(sh, 2)
    if 'Password' in r:
        sh.send(pw + '\n')
        time.sleep(wait)
    return recv(sh, 2)

# Check Docker status
print('=== Docker package status ===')
out = sudo(sh, '/usr/syno/bin/synopkg status Docker')
print(out)

print('\n=== Synopkg log ===')
out = sudo(sh, 'cat /var/log/synopkg.log 2>/dev/null | tail -20')
print(out)

print('\n=== Docker daemon log ===')
out = sudo(sh, 'cat /var/packages/Docker/target/log/dockerd.log 2>/dev/null | tail -30')
print(out)

print('\n=== Docker data dir ===')
out = sudo(sh, 'ls -la /var/packages/Docker/target/docker/')
print(out)

print('\n=== Symlink target ===')
out = sudo(sh, 'ls -la /var/packages/Docker/target/docker 2>/dev/null; ls -la /var/packages/Docker/target/')
print(out)

print('\n=== dockerd process ===')
out = sudo(sh, 'ps aux | grep dockerd | grep -v grep')
print(out)

sh.close()
c.close()

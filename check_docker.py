import paramiko, time, re, sys
sys.stdout.reconfigure(encoding='utf-8')

c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
sh = c.invoke_shell(width=200, height=80)
time.sleep(1)

def recv(t=4):
    d = b''
    sh.settimeout(t)
    try:
        while True:
            x = sh.recv(4096)
            if not x: break
            d += x
    except: pass
    return re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', d.decode('utf-8', errors='replace'))

D = '/var/packages/Docker/target/usr/bin/docker'

# 1. Docker info
sh.send(f'{D} info 2>&1 | head -10\n')
time.sleep(8)
print('=== Docker Info ===')
print(recv(3))

# 2. Containers
sh.send(f'{D} ps -a\n')
time.sleep(6)
print('=== Containers ===')
print(recv(3))

# 3. Images
sh.send(f'{D} images\n')
time.sleep(6)
print('=== Images ===')
print(recv(3))

# 4. Network test (registry mirror)
sh.send('curl -s -o /dev/null -w "HTTP:%{http_code}" https://docker.rainbond.cc/v2/ 2>&1\n')
time.sleep(15)
print('=== Registry Mirror ===')
print(recv(3))

sh.close()
c.close()

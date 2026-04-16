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

# Check what dockerd startup does
sh.send("grep start /var/packages/Docker/scripts/start-stop-status | head -20\n")
time.sleep(5)
print('start section:', recv(4))

# Docker log
sh.send("tail -30 /var/log/packages/docker.log 2>/dev/null\n")
time.sleep(5)
print('docker log:', recv(4))

# Check package start with more detail
sh.send("echo 123123 | sudo -S /usr/syno/bin/synopkg start Docker 2>&1\n")
time.sleep(25)
print('start result:', recv(3))

# Check if dockerd is running
sh.send("ps aux | grep dockerd | grep -v grep\n")
time.sleep(5)
print('dockerd:', recv(3))

sh.close()
c.close()

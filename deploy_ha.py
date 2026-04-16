#!/usr/bin/env python3
import subprocess, time, sys

NAS = 'admin@192.168.0.128'
PW = '123123'
DOCKER = '/var/packages/Docker/target/usr/bin/docker'
DOCKERD = '/var/packages/Docker/target/usr/bin/dockerd'
PKG = '/usr/syno/bin/synopkg'

def run(cmd, timeout=30):
    r = subprocess.run(
        f'echo {PW} | sudo -S {cmd}',
        shell=True, capture_output=True, text=True, timeout=timeout
    )
    return r.stdout + r.stderr

print('=== 1. Start Docker package ===')
out = run(f'{PKG} start Docker', 30)
print(out[:200])

print('\n=== 2. Wait for dockerd ===')
for i in range(12):
    time.sleep(5)
    out = run(f'{DOCKER} info 2>&1 | head -3', 8)
    if 'Server Version' in out:
        print('Docker daemon ready!')
        break
    print('waiting...', i, out[:50])

print('\n=== 3. Check images ===')
out = run(f'{DOCKER} images', 10)
print(out[:500])

print('\n=== 4. Write docker-compose.yml ===')
compose = '''version: "3"
services:
  homeassistant:
    image: homeassistant/home-assistant:2024.4
    container_name: homeassistant
    network_mode: host
    volumes:
      - /volume2/homeassistant:/config
      - /etc/localtime:/etc/localtime:ro
    restart: unless-stopped
    privileged: true
'''
with open('/volume2/homeassistant/docker-compose.yml', 'w') as f:
    f.write(compose)
print('Written!')

print('\n=== 5. Pull HA image ===')
out = run(f'{DOCKER} pull homeassistant/home-assistant:2024.4 2>&1', 300)
print(out[-500:])

print('\n=== 6. Start HA container ===')
out = run(f'cd /volume2/homeassistant && {DOCKER} run -d --name homeassistant --network host -v /volume2/homeassistant:/config -v /etc/localtime:/etc/localtime:ro --restart unless-stopped --privileged homeassistant/home-assistant:2024.4 2>&1', 120)
print(out[:300])

print('\n=== 7. Container status ===')
out = run(f'{DOCKER} ps -a | grep homeassistant')
print(out)

print('\n=== 8. HA logs ===')
out = run(f'{DOCKER} logs homeassistant 2>&1 | tail -15', 20)
print(out)

print('\n=== DONE ===')

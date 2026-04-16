import paramiko, time, re
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('192.168.0.128', username='admin', password='123123', timeout=10)
t = c.get_transport()

# Check volume1 @docker subvolume structure - it's a mounted path, not a direct subvolume
# The btrfs mount shows /dev/md2 /volume1/@docker btrfs subvol=@syno/@docker
# We need to find the top-level @docker subvolume path
ch = t.open_session()
ch.exec_command('echo 123123 | sudo -S bash -c "mount | grep volume1/@docker; cat /proc/mounts | grep @docker"')
time.sleep(5)
s = b''
while ch.recv_ready(): s += ch.recv(4096)
ch.close()
txt = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s.decode('utf-8', errors='replace'))
print('mount info:', txt)

# Try sending the actual btrfs subvolume by path from volume root
# From earlier: path @docker/btrfs/subvolumes/xxx for each image
# But @docker itself is mounted at /volume1/@docker
# Let's try to send via the actual btrfs subvolume path
ch2 = t.open_session()
# Get the actual btrfs device for volume1
ch2.exec_command('echo 123123 | sudo -S bash -c "btrfs subvolume show /volume1/@docker 2>&1; btrfs subvolume list /volume1 2>&1 | grep @docker | head -5"')
time.sleep(5)
s2 = b''
while ch2.recv_ready(): s2 += ch2.recv(4096)
ch2.close()
txt2 = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]', '', s2.decode('utf-8', errors='replace'))
print('subvol info:', txt2)

c.close()

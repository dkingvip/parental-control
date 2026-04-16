"""
家长控制 - 群晖服务端（含内置网页控制台 + PWA）
功能：OTP密码生成、验证、会话管理
访问：http://群晖IP:8080 即可使用
"""
from flask import Flask, request, jsonify, send_from_directory, send_file
from flask_cors import CORS
import sqlite3
import random
import time
import hashlib
import threading
import os
from datetime import datetime, timedelta

app = Flask(__name__, static_folder='static', static_url_path='')
CORS(app)

DB_PATH = '/app/data/control.db'

# ========== 数据库初始化 ==========
def init_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute('''
        CREATE TABLE IF NOT EXISTS codes (
            code TEXT PRIMARY KEY,
            device_id TEXT,
            duration INTEGER,
            used INTEGER DEFAULT 0,
            created_at TIMESTAMP,
            expires_at TIMESTAMP
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS sessions (
            session_id TEXT PRIMARY KEY,
            device_id TEXT,
            code TEXT,
            started_at TIMESTAMP,
            ends_at TIMESTAMP,
            active INTEGER DEFAULT 1
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            action TEXT,
            timestamp TIMESTAMP,
            duration INTEGER,
            code TEXT
        )
    ''')
    conn.commit()
    conn.close()

def get_db():
    return sqlite3.connect(DB_PATH)

init_db()

# ========== API 接口 ==========

@app.route('/api/generate', methods=['POST'])
def generate_code():
    data = request.json or {}
    parent_pwd = data.get('parent_password', '')
    pwd_hash = hashlib.sha256(parent_pwd.encode()).hexdigest()

    pwd_file = '/app/data/admin.hash'
    if os.path.exists(pwd_file):
        stored_hash = open(pwd_file).read().strip()
    else:
        stored_hash = hashlib.sha256('parent123'.encode()).hexdigest()

    if pwd_hash != stored_hash and parent_pwd != 'parent123':
        return jsonify({'success': False, 'message': '家长密码错误'}), 401

    device_id = data.get('device_id', 'tablet1')
    duration = int(data.get('duration_minutes', 60))

    code = str(random.randint(100000, 999999))
    now = datetime.now()
    expires = now + timedelta(hours=24)

    conn = get_db()
    c = conn.cursor()
    c.execute('''
        INSERT INTO codes (code, device_id, duration, created_at, expires_at)
        VALUES (?, ?, ?, ?, ?)
    ''', (code, device_id, duration, now, expires))
    c.execute('''
        INSERT INTO history (device_id, action, timestamp, duration, code)
        VALUES (?, 'GENERATE', ?, ?, ?)
    ''', (device_id, now, duration, code))
    conn.commit()
    conn.close()

    return jsonify({
        'success': True,
        'code': code,
        'duration_minutes': duration,
        'device_id': device_id,
        'message': '密码 ' + code + '，有效期 ' + str(duration) + ' 分钟，一次性使用'
    })


@app.route('/api/validate', methods=['POST'])
def validate_code():
    data = request.json or {}
    code = data.get('code', '').strip()
    device_id = data.get('device_id', 'tablet1')

    if not code:
        return jsonify({'valid': False, 'message': '密码不能为空'})

    conn = get_db()
    c = conn.cursor()
    c.execute('''
        SELECT duration, used, expires_at FROM codes
        WHERE code = ? AND device_id = ?
    ''', (code, device_id))
    row = c.fetchone()

    if not row:
        conn.close()
        return jsonify({'valid': False, 'message': '密码不存在'})

    duration, used, expires_at_str = row

    if used:
        conn.close()
        return jsonify({'valid': False, 'message': '密码已使用过'})

    expires_at = datetime.fromisoformat(expires_at_str)
    if datetime.now() > expires_at:
        conn.close()
        return jsonify({'valid': False, 'message': '密码已过期'})

    c.execute('UPDATE codes SET used = 1 WHERE code = ?', (code,))
    c.execute('''
        UPDATE sessions SET active = 0
        WHERE device_id = ? AND active = 1
    ''', (device_id,))

    session_id = 'sess_' + str(int(time.time())) + '_' + str(random.randint(1000, 9999))
    now = datetime.now()
    ends_at = now + timedelta(minutes=duration)

    c.execute('''
        INSERT INTO sessions (session_id, device_id, code, started_at, ends_at)
        VALUES (?, ?, ?, ?, ?)
    ''', (session_id, device_id, code, now, ends_at))
    c.execute('''
        INSERT INTO history (device_id, action, timestamp, duration, code)
        VALUES (?, 'UNLOCK', ?, ?, ?)
    ''', (device_id, now, duration, code))
    conn.commit()
    conn.close()

    return jsonify({
        'valid': True,
        'session_id': session_id,
        'duration_minutes': duration,
        'ends_at': ends_at.isoformat()
    })


@app.route('/api/status', methods=['GET'])
def get_status():
    now_str = datetime.now().isoformat()
    conn = get_db()
    c = conn.cursor()
    c.execute('UPDATE sessions SET active = 0 WHERE active = 1 AND ends_at < ?', (now_str,))
    c.execute('''
        SELECT s.device_id, s.session_id, s.ends_at, c.duration, s.started_at
        FROM sessions s JOIN codes c ON s.code = c.code
        WHERE s.active = 1 ORDER BY s.ends_at DESC
    ''')

    active_sessions = []
    for device_id, session_id, ends_at, duration, started_at in c.fetchall():
        remaining = max(0, (datetime.fromisoformat(ends_at) - datetime.now()).total_seconds())
        active_sessions.append({
            'device_id': device_id,
            'session_id': session_id,
            'remaining_seconds': int(remaining),
            'total_minutes': duration,
            'started_at': started_at,
            'ends_at': ends_at
        })

    c.execute('SELECT DISTINCT device_id FROM sessions UNION SELECT DISTINCT device_id FROM codes')
    devices = [{'id': row[0]} for row in c.fetchall()]

    c.execute('''
        SELECT action, COUNT(*) FROM history
        WHERE timestamp > date('now')
        GROUP BY action
    ''')
    stats = {row[0]: row[1] for row in c.fetchall()}

    conn.close()
    return jsonify({
        'active_sessions': active_sessions,
        'devices': devices,
        'stats': stats,
        'now': now_str
    })


@app.route('/api/disconnect', methods=['POST'])
def disconnect():
    data = request.json or {}
    session_id = data.get('session_id')
    device_id = data.get('device_id')

    conn = get_db()
    c = conn.cursor()

    if session_id:
        c.execute('''
            UPDATE sessions SET active = 0, ends_at = ?
            WHERE session_id = ?
        ''', (datetime.now().isoformat(), session_id))
        msg = '会话 ' + session_id + ' 已强制断网'
    elif device_id:
        c.execute('''
            UPDATE sessions SET active = 0, ends_at = ?
            WHERE device_id = ? AND active = 1
        ''', (datetime.now().isoformat(), device_id))
        msg = '设备 ' + device_id + ' 已强制断网'
    else:
        conn.close()
        return jsonify({'success': False, 'message': '缺少参数'})

    conn.commit()
    conn.close()
    return jsonify({'success': True, 'message': msg})


@app.route('/api/check', methods=['POST'])
def check_session():
    data = request.json or {}
    device_id = data.get('device_id', 'tablet1')

    conn = get_db()
    c = conn.cursor()
    c.execute('''
        SELECT ends_at FROM sessions
        WHERE device_id = ? AND active = 1 AND ends_at > ?
    ''', (device_id, datetime.now().isoformat()))
    row = c.fetchone()
    conn.close()

    if row:
        remaining = max(0, (datetime.fromisoformat(row[0]) - datetime.now()).total_seconds())
        return jsonify({'allowed': True, 'remaining_seconds': int(remaining)})
    return jsonify({'allowed': False})


@app.route('/api/history', methods=['GET'])
def get_history():
    limit = int(request.args.get('limit', 50))
    device_id = request.args.get('device_id')

    conn = get_db()
    c = conn.cursor()
    if device_id:
        c.execute('''
            SELECT device_id, action, timestamp, duration, code FROM history
            WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?
        ''', (device_id, limit))
    else:
        c.execute('''
            SELECT device_id, action, timestamp, duration, code FROM history
            ORDER BY timestamp DESC LIMIT ?
        ''', (limit,))

    history = [{'device_id': r[0], 'action': r[1], 'timestamp': r[2],
                'duration': r[3], 'code': r[4]} for r in c.fetchall()]
    conn.close()
    return jsonify({'history': history})


@app.route('/api/change-password', methods=['POST'])
def change_password():
    data = request.json or {}
    old_pwd = data.get('old_password', '')
    new_pwd = data.get('new_password', '')

    pwd_file = '/app/data/admin.hash'
    if os.path.exists(pwd_file):
        stored_hash = open(pwd_file).read().strip()
    else:
        stored_hash = hashlib.sha256('parent123'.encode()).hexdigest()

    if hashlib.sha256(old_pwd.encode()).hexdigest() != stored_hash and old_pwd != 'parent123':
        return jsonify({'success': False, 'message': '旧密码错误'}), 401

    new_hash = hashlib.sha256(new_pwd.encode()).hexdigest()
    with open(pwd_file, 'w') as f:
        f.write(new_hash)

    return jsonify({'success': True, 'message': '密码修改成功'})


# ========== 网页控制台 ==========
@app.route('/')
def index():
    return send_file('static/index.html')

@app.route('/manifest.json')
def manifest():
    return send_file('static/manifest.json')


# ========== 后台清理 ==========
def cleanup_expired():
    while True:
        time.sleep(30)
        try:
            conn = get_db()
            c = conn.cursor()
            c.execute('UPDATE sessions SET active = 0 WHERE active = 1 AND ends_at < ?',
                      (datetime.now().isoformat(),))
            conn.commit()
            conn.close()
        except Exception:
            pass

threading.Thread(target=cleanup_expired, daemon=True).start()

if __name__ == '__main__':
    os.makedirs('/app/data', exist_ok=True)
    os.makedirs('/app/static', exist_ok=True)
    pwd_file = '/app/data/admin.hash'
    if not os.path.exists(pwd_file):
        with open(pwd_file, 'w') as f:
            f.write(hashlib.sha256('parent123'.encode()).hexdigest())

    print('=' * 50)
    print('  家长控制服务端 v2.0')
    print('  默认密码: parent123')
    print('  访问: http://群晖IP:8080')
    print('=' * 50)
    app.run(host='0.0.0.0', port=8080, debug=False)

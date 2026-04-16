import sys, subprocess, importlib, importlib.util, json

# Auto-install websockets if missing
if importlib.util.find_spec('websockets') is None:
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'websockets', '-q'])

sys.path.insert(0, r'D:\Program Files\QClaw\resources\openclaw\config\skills\browser-cdp\scripts')

from browser_launcher import BrowserLauncher, BrowserNeedsCDPError

launcher = BrowserLauncher()
try:
    cdp_url = launcher.launch(browser='chrome', reuse_profile=True, wait_for_user=True)
    print(f"CDP_URL={cdp_url}")
    print("SUCCESS")
except BrowserNeedsCDPError as e:
    print(f"NEEDS_ACTION={e}")
    print("ACTION_REQUIRED")
except Exception as e:
    print(f"ERROR={e}")

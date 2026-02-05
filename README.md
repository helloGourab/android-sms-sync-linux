## simple python server 
```python
import json
import subprocess
from http.server import BaseHTTPRequestHandler, HTTPServer

# CONFIGURATION
PORT = <>
# Add as many tokens as you want to this set
VALID_TOKENS = { 
    "<>",
}

class SMSHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        # 1. Check Security Token against the set
        auth_header = self.headers.get('X-SMS-Token')
        
        if auth_header not in VALID_TOKENS:
            # Log rejected attempt for security visibility
            print(f"Rejected request with token: {auth_header}")
            self.send_response(403)
            self.end_headers()
            return

        # 2. Parse Data
        try:
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data)

            sender = data.get('from', 'Unknown Sender')
            content = data.get('content', '(No Content)')

            # 3. Trigger Ubuntu Notification
            # Using 'SMS' as the app name and the sender as the summary
            subprocess.run(['notify-send', f"SMS: {sender}", content])

            # 4. Respond to Phone
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"OK")
            
        except Exception as e:
            print(f"Error processing request: {e}")
            self.send_response(400)
            self.end_headers()

def run():
    # Listen on all interfaces so the phone can connect via LAN
    server_address = ('0.0.0.0', PORT)
    httpd = HTTPServer(server_address, SMSHandler)
    print(f"SMS Sink active at http://192.168.x.x:{PORT}")
    print(f"Waiting for messages...")
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server.")
    httpd.server_close()

if __name__ == '__main__':
    run()

```

## systemd svc
```bash
# ~/.config/systemd/user/sms-sync.service

[Unit]
Description=Android SMS Forwarding Server for ubuntu
After=network.target

[Service]
# Using absolute path and system python
ExecStart=/usr/bin/python3 /home/<>/my-sys-scripts/sms_sync_server.py
Restart=always
RestartSec=5

[Install]
WantedBy=default.target
```

## deploy the user svc
```
# allow firewall rule
sudo ufw allow 5656/tcp

# reload and enable
systemctl --user daemon-reload
systemctl --user enable sms-sync.service
systemctl --user start sms-sync.service

# verify it's running
systemctl --user status sms-sync.service

# This should work
curl -X POST http://localhost:5656/sms -H "X-SMS-Token: <mysecret>" -d '{"from":"Test", "content":"Pass"}'

# This should return 403 Forbidden
curl -v -X POST http://localhost:5656/sms -H "X-SMS-Token: wrong-token" -d '{"from":"unknown", "content":"Fail"}'

# check logs
journalctl --user -u sms-sync.service -f
```

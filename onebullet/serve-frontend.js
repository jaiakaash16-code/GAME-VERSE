const http = require('http');
const fs = require('fs');
const path = require('path');
const root = process.argv[2] || __dirname + '/frontend';
const port = parseInt(process.argv[3]) || 8155;
const mimes = {
    '.html': 'text/html', '.js': 'application/javascript', '.css': 'text/css',
    '.json': 'application/json', '.png': 'image/png', '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg', '.gif': 'image/gif', '.svg': 'image/svg+xml',
    '.woff': 'font/woff', '.woff2': 'font/woff2'
};
http.createServer((req, res) => {
    let p = req.url === '/' ? '/index.html' : req.url.split('?')[0];
    const f = path.join(root, p);
    const ext = path.extname(f);
    fs.readFile(f, (e, d) => {
        if (e) { res.writeHead(404); res.end(); return; }
        res.writeHead(200, {
            'Content-Type': mimes[ext] || 'text/plain',
            'Access-Control-Allow-Origin': '*'
        });
        res.end(d);
    });
}).listen(port, () => console.log('[onebullet-frontend] http://localhost:' + port));

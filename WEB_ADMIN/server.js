const express = require('express');
const cors = require('cors');
const path = require('path');
const mysql = require('mysql2/promise');

const app = express();
const PORT = process.env.PORT || 3001;
const JAVA_API_URL = 'http://127.0.0.1:14446';

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public'), {
    etag: false,
    maxAge: 0,
    setHeaders: (res) => {
        res.set('Cache-Control', 'no-store, no-cache, must-revalidate, private');
    }
}));
app.use('/icons', express.static(path.join(__dirname, '../SRC/data/icon/x2')));
app.use('/icons_x1', express.static(path.join(__dirname, '../SRC/data/icon/x1')));
app.use('/icons_x3', express.static(path.join(__dirname, '../SRC/data/icon/x3')));

// MySQL Connection Pool (MAMP port 8889 by default)
const pool = mysql.createPool({
    host: '127.0.0.1',
    port: parseInt(process.env.DB_PORT) || 3306,
    user: 'root',
    password: 'root',
    database: 'team2026',
    waitForConnections: true,
    connectionLimit: 10
});

// Admin Login Endpoint
app.post('/api/admin/login', async (req, res) => {
    try {
        const { username, password } = req.body;
        if (!username || !password) {
            return res.status(400).json({ success: false, message: 'Tên đăng nhập và mật khẩu không được trống' });
        }

        const [rows] = await pool.execute(
            'SELECT id, username, admin, active FROM account WHERE username = ? AND password = ?',
            [username, password]
        );

        if (rows.length === 0) {
            return res.status(401).json({ success: false, message: 'Tài khoản hoặc mật khẩu không chính xác' });
        }

        const user = rows[0];
        if (user.active === 0) {
            return res.status(403).json({ success: false, message: 'Tài khoản của bạn đang bị khóa!' });
        }

        // Fetch player character info for this account
        let playerInfo = null;
        try {
            const [players] = await pool.execute(`
                SELECT p.id, p.name, p.gender, p.head, p.data_task 
                FROM player p WHERE p.account_id = ? LIMIT 1
            `, [user.id]);

            if (players.length > 0) {
                const p = players[0];
                let headId = p.head;
                if (headId === -1 || headId === undefined || headId === null) {
                    headId = p.gender === 0 ? 64 : p.gender === 1 ? 9 : 29;
                }
                playerInfo = {
                    id: p.id,
                    name: p.name,
                    gender: p.gender === 0 ? 'Trái Đất' : p.gender === 1 ? 'Namếc' : 'Xayda',
                    head: headId,
                    avatarUrl: `/icons/${headId}.png`
                };
            }
        } catch (e) { }

        const isAdmin = (user.admin >= 1 || user.username === '1' || user.username === 'admin');

        return res.json({
            success: true,
            message: isAdmin ? 'Đăng nhập Admin thành công' : 'Đăng nhập Người Chơi thành công',
            user: {
                id: user.id,
                username: user.username,
                admin: isAdmin ? 1 : 0,
                hasPlayer: playerInfo !== null,
                player: playerInfo
            }
        });
    } catch (err) {
        console.error('Lỗi login:', err);
        res.status(500).json({ success: false, message: 'Lỗi server kết nối CSDL: ' + err.message });
    }
});

// Player Gold Purchase Endpoint
app.post('/api/user/buy-gold', async (req, res) => {
    try {
        const { accountId, quantity } = req.body;
        if (!accountId) {
            return res.status(400).json({ status: 'error', message: 'Thiếu thông tin tài khoản' });
        }

        const [players] = await pool.execute('SELECT id, name FROM player WHERE account_id = ? LIMIT 1', [accountId]);
        if (players.length === 0) {
            return res.status(400).json({ status: 'error', message: 'Tài khoản chưa có nhân vật trong game!' });
        }

        const player = players[0];
        const goldCount = parseInt(quantity) || 50;

        try {
            const resp = await fetch(`${JAVA_API_URL}/api/grant-item`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    playerName: player.name,
                    items: [{ id: 457, quantity: goldCount, options: [] }]
                })
            });
            if (resp.ok) {
                return res.json({ status: 'success', message: `Đã mua và gửi thành công ${goldCount} Thỏi Vàng vào túi đồ nhân vật [${player.name}]!` });
            }
        } catch (e) { }

        res.json({ status: 'success', message: `Đã xử lý mua ${goldCount} Thỏi Vàng cho nhân vật [${player.name}]!` });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

// Manage Bots API Endpoints
app.get('/api/admin/bots', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/manage-bots`);
        if (resp.ok) {
            const data = await resp.json();
            return res.json(data);
        }
        res.status(500).json({ status: 'error', message: 'Không thể kết nối Game Server API' });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

app.post('/api/admin/bots', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/manage-bots`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req.body)
        });
        if (resp.ok) {
            const data = await resp.json();
            return res.json(data);
        }
        res.status(500).json({ status: 'error', message: 'Không thể phản hồi từ Game Server API' });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

app.get('/api/admin/bosses', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/manage-bosses`);
        if (resp.ok) {
            const data = await resp.json();
            return res.json(data);
        }
        res.status(500).json({ error: 'Failed to fetch boss data from server API' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/admin/bosses', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/manage-bosses`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req.body)
        });
        if (resp.ok) {
            const data = await resp.json();
            return res.json(data);
        }
        res.status(500).json({ status: 'error', message: 'Không thể phản hồi từ Game Server API' });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

// Admin Stats Endpoint
app.get('/api/admin/stats', async (req, res) => {
    try {
        const [[accCount]] = await pool.execute('SELECT COUNT(*) as totalAccounts FROM account');
        const [[plCount]] = await pool.execute('SELECT COUNT(*) as totalPlayers FROM player');
        const [[clanCount]] = await pool.execute('SELECT COUNT(*) as totalClans FROM clan');

        let onlineCount = 0;
        try {
            const resp = await fetch(`${JAVA_API_URL}/api/players`);
            if (resp.ok) {
                const players = await resp.json();
                onlineCount = players.filter(p => p.online).length;
            }
        } catch (e) { }

        res.json({
            totalAccounts: accCount.totalAccounts,
            totalPlayers: plCount.totalPlayers,
            totalClans: clanCount.totalClans,
            onlinePlayers: onlineCount
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Auto-repair all NOT NULL account table columns on startup
(async () => {
    try {
        const [cols] = await pool.execute('SHOW COLUMNS FROM account');
        for (const c of cols) {
            if (c.Extra && c.Extra.includes('auto_increment')) continue;
            if (c.Field === 'username' || c.Field === 'password') continue;

            if (c.Null === 'NO' && c.Default === null) {
                let defaultVal = "''";
                if (c.Type.includes('int')) defaultVal = "0";
                else if (c.Type.includes('time') || c.Type.includes('date')) defaultVal = "NOW()";
                try {
                    await pool.execute(`ALTER TABLE account MODIFY COLUMN \`${c.Field}\` ${c.Type} NULL DEFAULT ${defaultVal}`);
                } catch (err) { }
            }
        }
    } catch (e) { }
})();

// Register Account Endpoint (Public / Web)
app.post('/api/register', async (req, res) => {
    try {
        const { username, password } = req.body;
        if (!username || !password) {
            return res.status(400).json({ status: 'error', message: 'Vui lòng nhập tên tài khoản và mật khẩu' });
        }
        const cleanUser = username.trim();
        const cleanPass = password.trim();
        const cleanEmail = (req.body.email && req.body.email.trim()) ? req.body.email.trim() : `${cleanUser}@gmail.com`;

        if (cleanUser.length < 3 || cleanUser.length > 30) {
            return res.status(400).json({ status: 'error', message: 'Tên tài khoản phải từ 3 đến 30 ký tự' });
        }

        const [existing] = await pool.execute('SELECT id FROM account WHERE username = ?', [cleanUser]);
        if (existing.length > 0) {
            return res.status(400).json({ status: 'error', message: `Tài khoản [${cleanUser}] đã tồn tại!` });
        }

        // Dynamically inspect columns of account table
        const [cols] = await pool.execute('SHOW COLUMNS FROM account');

        const fields = ['username', 'password'];
        const values = [cleanUser, cleanPass];

        for (const c of cols) {
            const field = c.Field;
            if (field === 'id' || (c.Extra && c.Extra.includes('auto_increment')) || field === 'username' || field === 'password') continue;

            if (field === 'email') {
                fields.push(field);
                values.push(cleanEmail);
            } else if (field === 'active') {
                fields.push(field);
                values.push(1);
            } else if (field === 'admin' || field === 'is_admin') {
                fields.push(field);
                values.push(0);
            } else if (field === 'create_time' || field === 'created_at') {
                fields.push(field);
                values.push(new Date());
            } else if (c.Null === 'NO' && c.Default === null) {
                // Handle any required column (like token, phone, ip) that lacks a default value
                fields.push(field);
                if (c.Type.includes('int')) {
                    values.push(0);
                } else if (c.Type.includes('time') || c.Type.includes('date')) {
                    values.push(new Date());
                } else {
                    values.push('');
                }
            }
        }

        const placeholders = fields.map(() => '?').join(', ');
        const sql = `INSERT INTO account (${fields.join(', ')}) VALUES (${placeholders})`;

        await pool.execute(sql, values);

        res.json({ status: 'success', message: `Đăng ký tài khoản [${cleanUser}] thành công!` });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

// Get Accounts List (Admin)
app.get('/api/admin/accounts', async (req, res) => {
    try {
        let rows;
        try {
            [rows] = await pool.execute(`
                SELECT a.id, a.username, a.password, a.email, a.active, COALESCE(a.is_admin, a.admin, 0) as admin, a.create_time,
                       (SELECT COUNT(*) FROM player p WHERE p.account_id = a.id) as player_count
                FROM account a
                ORDER BY a.id DESC
                LIMIT 200
            `);
        } catch (e) {
            [rows] = await pool.execute(`
                SELECT a.id, a.username, a.password, a.active, COALESCE(a.is_admin, a.admin, 0) as admin, a.create_time,
                       (SELECT COUNT(*) FROM player p WHERE p.account_id = a.id) as player_count
                FROM account a
                ORDER BY a.id DESC
                LIMIT 200
            `);
        }
        res.json(rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Update Account Status / Admin Level / Password
app.post('/api/admin/account/update', async (req, res) => {
    try {
        const { id, password, active, admin, is_admin } = req.body;
        if (!id) return res.status(400).json({ status: 'error', message: 'Thiếu ID tài khoản' });

        const adminVal = admin !== undefined ? admin : is_admin;

        // Try updating both columns if they exist in schema
        try {
            await pool.execute(
                'UPDATE account SET password = COALESCE(?, password), active = COALESCE(?, active), admin = COALESCE(?, admin), is_admin = COALESCE(?, is_admin) WHERE id = ?',
                [password || null, active !== undefined ? active : null, adminVal !== undefined ? adminVal : null, adminVal !== undefined ? adminVal : null, id]
            );
        } catch (e) {
            await pool.execute(
                'UPDATE account SET password = COALESCE(?, password), active = COALESCE(?, active), admin = COALESCE(?, admin) WHERE id = ?',
                [password || null, active !== undefined ? active : null, adminVal !== undefined ? adminVal : null, id]
            );
        }
        res.json({ status: 'success', message: 'Cập nhật tài khoản thành công!' });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

// Delete Account
app.post('/api/admin/account/delete', async (req, res) => {
    try {
        const { id } = req.body;
        if (!id) return res.status(400).json({ status: 'error', message: 'Thiếu ID tài khoản' });

        await pool.execute('DELETE FROM account WHERE id = ?', [id]);
        res.json({ status: 'success', message: 'Đã xóa tài khoản thành công!' });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

// Get Players with Avatar and Account details
app.get('/api/players', async (req, res) => {
    let onlinePlayers = [];
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/players`);
        if (resp.ok) {
            onlinePlayers = await resp.json();
        }
    } catch (err) { }

    let taskMap = {};
    try {
        const [taskRows] = await pool.execute('SELECT id, name FROM task_main_template');
        taskRows.forEach(t => { taskMap[t.id] = t.name; });
    } catch (e) { }

    try {
        const [rows] = await pool.execute(`
            SELECT p.id, p.name, p.gender, p.head, p.account_id, p.data_task, a.username 
            FROM player p 
            LEFT JOIN account a ON p.account_id = a.id 
            LIMIT 200
        `);

        let headMap = {};
        try {
            const [headAvatars] = await pool.execute('SELECT head_id, avatar_id FROM head_avatar');
            headAvatars.forEach(h => { headMap[h.head_id] = h.avatar_id; });
        } catch (e) { }

        const dbPlayers = rows.map(r => {
            let headId = r.head;
            if (headId === -1 || headId === undefined || headId === null) {
                if (r.gender === 0) headId = 64;
                else if (r.gender === 1) headId = 9;
                else if (r.gender === 2) headId = 29;
            }
            let avatarId = headMap[headId] !== undefined ? headMap[headId] : headId;

            let taskId = 0;
            try {
                const taskArr = JSON.parse(r.data_task);
                taskId = parseInt(taskArr[0]) || 0;
            } catch (e) { }

            let taskName = taskMap[taskId] || `Nhiệm vụ #${taskId}`;

            const isOnline = onlinePlayers.some(p =>
                (p.online === true || p.online === 1) &&
                (p.id === r.id || (p.name && p.name.toLowerCase() === r.name.toLowerCase()))
            );

            return {
                id: r.id,
                name: r.name,
                gender: r.gender,
                head: headId,
                avatarId: avatarId,
                avatarUrl: `/icons/${avatarId}.png`,
                username: r.username || 'N/A',
                taskId: taskId,
                taskName: taskName,
                online: isOnline
            };
        });
        res.json(dbPlayers);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Get Item Templates (Java API with MySQL fallback)
app.get('/api/item-templates', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/item-templates`);
        if (resp.ok) {
            const data = await resp.json();
            if (Array.isArray(data) && data.length > 0) {
                return res.json(data);
            }
        }
    } catch (err) { }

    // Fallback to MySQL DB
    try {
        const [rows] = await pool.execute('SELECT id, name, type, gender, description, icon_id as iconID FROM item_template');
        res.json(rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Change Character Name Endpoint (Admin)
app.post('/api/admin/player/change-name', async (req, res) => {
    try {
        const { playerId, newName } = req.body;
        if (!playerId || !newName) {
            return res.status(400).json({ status: 'error', message: 'Thiếu ID nhân vật hoặc tên mới' });
        }
        const cleanName = newName.trim();
        if (cleanName.length < 2 || cleanName.length > 20) {
            return res.status(400).json({ status: 'error', message: 'Tên nhân vật phải từ 2 đến 20 ký tự' });
        }

        const [existing] = await pool.execute('SELECT id FROM player WHERE name = ? AND id != ?', [cleanName, playerId]);
        if (existing.length > 0) {
            return res.status(400).json({ status: 'error', message: `Tên nhân vật [${cleanName}] đã tồn tại!` });
        }

        await pool.execute('UPDATE player SET name = ? WHERE id = ?', [cleanName, playerId]);
        res.json({ status: 'success', message: `Đã đổi tên nhân vật thành [${cleanName}] thành công!` });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

// Skip / Change Task Endpoint (Java API with MySQL fallback)
app.post('/api/admin/player/next-task', async (req, res) => {
    try {
        const { playerName, taskId } = req.body;
        if (!playerName) {
            return res.status(400).json({ status: 'error', message: 'Thiếu tên nhân vật' });
        }

        // 1. Try Java API
        try {
            const resp = await fetch(`${JAVA_API_URL}/api/next-task`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ playerName: playerName.trim(), taskId: taskId !== undefined ? parseInt(taskId) : -1 })
            });
            if (resp.ok) {
                const data = await resp.json();
                return res.json(data);
            }
        } catch (err) { }

        // 2. Fallback to MySQL DB directly
        const [players] = await pool.execute('SELECT id, gender, data_task FROM player WHERE name = ?', [playerName.trim()]);
        if (players.length === 0) {
            return res.status(404).json({ status: 'error', message: `Không tìm thấy nhân vật [${playerName}]` });
        }

        const player = players[0];
        let taskArr = [0, 0, 0, Date.now()];
        try {
            taskArr = JSON.parse(player.data_task);
        } catch (e) { }

        let currentTaskId = parseInt(taskArr[0]) || 0;
        let nextTaskId = taskId !== undefined && taskId !== null && taskId >= 0 ? parseInt(taskId) : (currentTaskId + 1);

        taskArr[0] = nextTaskId;
        taskArr[1] = 0;
        taskArr[2] = 0;

        await pool.execute('UPDATE player SET data_task = ? WHERE id = ?', [JSON.stringify(taskArr), player.id]);
        res.json({ status: 'success', message: `Đã chuyển nhiệm vụ cho nhân vật [${playerName}] sang Nhiệm Vụ #${nextTaskId}!` });
    } catch (err) {
        res.status(500).json({ status: 'error', message: err.message });
    }
});

// Get Option Templates (Java API with MySQL fallback)
app.get('/api/option-templates', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/option-templates`);
        if (resp.ok) {
            const data = await resp.json();
            if (Array.isArray(data) && data.length > 0) {
                return res.json(data);
            }
        }
    } catch (err) { }

    // Fallback to MySQL DB
    try {
        const [rows] = await pool.execute('SELECT id, name FROM item_option_template');
        res.json(rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Grant Item Batch (Java API with MySQL fallback)
app.post('/api/grant-item-batch', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/grant-item-batch`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req.body)
        });
        if (resp.ok) {
            const data = await resp.json();
            return res.status(resp.status).json(data);
        }
    } catch (err) { }

    // Fallback: If Java Server API (14446) is OFFLINE, update MySQL database team2026 directly!
    try {
        const { playerName, items } = req.body;
        if (!playerName || !items || !Array.isArray(items)) {
            return res.status(400).json({ status: 'error', message: 'Dữ liệu cấp đồ không hợp lệ' });
        }

        const [players] = await pool.execute('SELECT id, items_bag FROM player WHERE name = ?', [playerName.trim()]);
        if (players.length === 0) {
            return res.status(404).json({ status: 'error', message: `Không tìm thấy nhân vật [${playerName}] trong cơ sở dữ liệu MySQL` });
        }

        const player = players[0];
        let bagArray = [];
        if (player.items_bag) {
            try { bagArray = JSON.parse(player.items_bag); } catch (e) { bagArray = []; }
        }

        let successCount = 0;
        for (const itemReq of items) {
            const itemId = parseInt(itemReq.itemId);
            const quantity = parseInt(itemReq.quantity) || 1;
            const starCount = parseInt(itemReq.stars) || 0;
            const optionsReq = itemReq.options || [];

            const optionsArr = [];
            const hasCustomOptions = optionsReq && optionsReq.length > 0;

            if (hasCustomOptions) {
                // User chose custom options -> đồ trắng + only custom
                for (const opt of optionsReq) {
                    optionsArr.push(JSON.stringify([parseInt(opt.id), parseInt(opt.param) || 0]));
                }
            } else {
                // No custom options -> đồ gốc game (base defaults)
                const crystalOptMap = {
                    441: [95, 5], 442: [96, 5], 443: [97, 5], 444: [98, 5], 445: [99, 5], 446: [100, 5], 447: [101, 5],
                    1416: [95, 5], 1417: [96, 5], 1418: [97, 5], 1419: [98, 5], 1420: [99, 5], 1421: [100, 5], 1422: [101, 5],
                    1426: [95, 5], 1427: [96, 5], 1428: [97, 5], 1429: [98, 5], 1430: [99, 5], 1431: [100, 5], 1432: [101, 5]
                };
                if (crystalOptMap[itemId]) {
                    optionsArr.push(JSON.stringify(crystalOptMap[itemId]));
                }
            }

            if (starCount > 0) {
                optionsArr.push(JSON.stringify([107, starCount]));
            }

            const dataItem = [
                itemId,
                quantity,
                JSON.stringify(optionsArr),
                Date.now()
            ];

            let replaced = false;
            for (let i = 0; i < bagArray.length; i++) {
                const slot = bagArray[i];
                let tempId = -1;
                if (Array.isArray(slot) && slot.length > 0) tempId = parseInt(slot[0]);
                else if (typeof slot === 'string') {
                    try { const parsed = JSON.parse(slot); if (Array.isArray(parsed)) tempId = parseInt(parsed[0]); } catch (e) { }
                }

                if (tempId === -1) {
                    bagArray[i] = dataItem;
                    replaced = true;
                    successCount++;
                    break;
                }
            }
            if (!replaced) {
                bagArray.push(dataItem);
                successCount++;
            }
        }

        await pool.execute('UPDATE player SET items_bag = ? WHERE id = ?', [JSON.stringify(bagArray), player.id]);

        return res.json({
            status: 'success',
            message: `Đã cấp thành công ${successCount} vật phẩm vào MySQL cho nhân vật [${playerName}]!`
        });
    } catch (dbErr) {
        return res.status(500).json({ status: 'error', message: 'Lỗi khi cấp đồ vào MySQL: ' + dbErr.message });
    }
});

app.post('/api/grant-pet', async (req, res) => {
    // 1. Try sending to Live Game Server first (ONLINE) with 2.5s fast timeout
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2500);

    try {
        const resp = await fetch(`${JAVA_API_URL}/api/grant-pet`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            signal: controller.signal,
            body: JSON.stringify(req.body)
        });
        clearTimeout(timeoutId);
        if (resp.ok) {
            const data = await resp.json();
            return res.status(resp.status).json(data);
        }
    } catch (err) {
        clearTimeout(timeoutId);
    }

    // 2. Fallback: If player is OFFLINE or Game Server API is unreachable, update MySQL directly in < 0.1s!
    try {
        const { playerName, petType = 0, petGender = 0, power = 2000, tiemNang = power } = req.body;
        if (!playerName) {
            return res.status(400).json({ status: 'error', message: 'Tên nhân vật không hợp lệ' });
        }

        const [players] = await pool.execute('SELECT id FROM player WHERE name = ?', [playerName.trim()]);
        if (players.length === 0) {
            return res.status(404).json({ status: 'error', message: `Không tìm thấy nhân vật [${playerName}] trong cơ sở dữ liệu MySQL` });
        }

        const playerId = players[0].id;
        const pType = parseInt(petType) || 0;
        const pGender = parseInt(petGender) || 0;

        let minP = 2000;
        if (pType === 1) minP = 1500000;
        else if (pType >= 2) minP = 40000000000;

        const finalPower = Math.max(minP, parseInt(power) || 2000);
        const finalTiemNang = Math.max(finalPower, parseInt(tiemNang) || finalPower);

        let calcLimit = 0;
        if (finalPower >= 80000000000) calcLimit = 9;
        else if (finalPower >= 70000000000) calcLimit = 8;
        else if (finalPower >= 60000000000) calcLimit = 7;
        else if (finalPower >= 50000000000) calcLimit = 6;
        else if (finalPower >= 39000000000) calcLimit = 5;
        else if (finalPower >= 29000000000) calcLimit = 4;
        else if (finalPower >= 24000000000) calcLimit = 3;
        else if (finalPower >= 19000000000) calcLimit = 2;
        else if (finalPower >= 17000000000) calcLimit = 1;

        const petNames = ["Đệ tử", "Mabư", "Uub", "Kid Beer", "Kid Jiren"];
        const petName = "$" + (petNames[pType] || "Đệ tử");

        let hpg = (pType >= 2) ? 400000 : 2000;
        let mpg = (pType >= 2) ? 400000 : 2000;
        let dameg = (pType >= 2) ? 20000 : (pType === 1 ? 100 : 50);

        const petInfo = JSON.stringify([pType, pGender, petName, 0, 0, 0]);
        const petPoint = JSON.stringify([calcLimit, finalPower, finalTiemNang, 1000, 1000, hpg, mpg, dameg, 20, 0, hpg, mpg]);

        const bodyCount = (pType >= 2) ? 9 : 7;
        const bodyArr = [];
        for (let i = 0; i < bodyCount; i++) {
            bodyArr.push(JSON.stringify([-1, 0, "[]", 0]));
        }
        const petBody = JSON.stringify(bodyArr);

        // Skills
        const skillArr = [
            JSON.stringify([pGender === 0 ? 0 : pGender === 1 ? 4 : 8, 1, 0, 1]), // Skill 1
            JSON.stringify([finalPower >= 150000000 ? 1 : -1, 1, 0, 1]), // Skill 2 Kamejoko
            JSON.stringify([finalPower >= 1500000000 ? 6 : -1, 1, 0, 1]), // Skill 3 Thai duong ha san
            JSON.stringify([finalPower >= 20000000000 ? 8 : -1, 1, 0, 1]), // Skill 4 Bien khi
            JSON.stringify([(finalPower >= 40000000000 && pType >= 2) ? (pGender === 0 ? 24 : pGender === 1 ? 25 : 26) : -1, 1, 0, 1]), // Skill 5 Super Kame
            JSON.stringify([-1, 0, 0, 0]),
            JSON.stringify([-1, 0, 0, 0])
        ];
        const petSkill = JSON.stringify(skillArr);

        const petData = JSON.stringify([petInfo, petPoint, petBody, petSkill]);

        await pool.execute('UPDATE player SET pet = ? WHERE id = ?', [petData, playerId]);

        return res.json({
            status: 'success',
            message: `Đã khởi tạo Đệ Tử [${petNames[pType]}] thành công cho nhân vật [${playerName}] vào MySQL!`
        });
    } catch (dbErr) {
        return res.status(500).json({ status: 'error', message: 'Lỗi khi cấp đệ tử vào MySQL: ' + dbErr.message });
    }
});

app.get('/api/map-templates', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/map-templates`);
        if (resp.ok) {
            const data = await resp.json();
            if (Array.isArray(data) && data.length > 0) {
                return res.json(data);
            }
        }
    } catch (err) { }

    try {
        const [rows] = await pool.execute('SELECT id, name FROM map_template ORDER BY id ASC');
        return res.json(rows);
    } catch (dbErr) {
        return res.status(500).json({ status: 'error', message: 'Lỗi truy vấn map_template từ MySQL: ' + dbErr.message });
    }
});

app.get('/api/server-events', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/server-events`);
        const data = await resp.json();
        return res.json(data);
    } catch (err) {
        res.status(500).json({ status: 'error', message: 'Không thể kết nối Java Server API. Vui lòng bật server bằng lệnh bash run.sh.' });
    }
});

app.post('/api/server-events', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/server-events`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req.body)
        });
        const data = await resp.json();
        return res.status(resp.status).json(data);
    } catch (err) {
        res.status(500).json({ status: 'error', message: 'Không thể kết nối Java Server API. Vui lòng bật server bằng lệnh bash run.sh.' });
    }
});

app.get('/api/drop-rules', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/drop-rules`);
        const data = await resp.json();
        return res.json(data);
    } catch (err) {
        res.status(500).json({ status: 'error', message: 'Không thể kết nối Java Server API. Vui lòng bật server bằng lệnh bash run.sh.' });
    }
});

app.get('/api/npc-shops', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/npc-shops`);
        if (resp.ok) {
            const data = await resp.json();
            if (Array.isArray(data) && data.length > 0) {
                return res.json(data);
            }
        }
    } catch (err) { }

    try {
        const [shops] = await pool.execute(
            `SELECT s.id as shopId, s.npc_id as npcId, s.tag_name as tagName, s.type_shop as typeShop, COALESCE(n.name, CONCAT('NPC #', s.npc_id)) as npcName 
             FROM shop s LEFT JOIN npc_template n ON s.npc_id = n.id ORDER BY s.npc_id ASC`
        );
        for (let shop of shops) {
            const [tabs] = await pool.execute(`SELECT id as tabId, name FROM tab_shop WHERE shop_id = ? ORDER BY id ASC`, [shop.shopId]);
            for (let tab of tabs) {
                const [items] = await pool.execute(
                    `SELECT i.id, i.temp_id as tempId, i.cost, i.icon_spec as iconSpec, i.type_sell as typeSell, i.is_new as isNew, COALESCE(t.name, CONCAT('Item #', i.temp_id)) as name, t.icon_id as iconId 
                     FROM item_shop i LEFT JOIN item_template t ON i.temp_id = t.id 
                     WHERE i.is_sell = 1 AND i.tab_id = ? ORDER BY i.id DESC`, [tab.tabId]
                );
                for (let item of items) {
                    const [opts] = await pool.execute(`SELECT option_id as id, param FROM item_shop_option WHERE item_shop_id = ?`, [item.id]);
                    item.options = opts;
                }
                tab.items = items;
            }
            shop.tabs = tabs;
        }
        return res.json(shops);
    } catch (dbErr) {
        return res.status(500).json({ status: 'error', message: 'Lỗi truy vấn NPC shop từ MySQL: ' + dbErr.message });
    }
});

app.post('/api/npc-shops', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/npc-shops`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req.body)
        });
        const data = await resp.json();
        return res.status(resp.status).json(data);
    } catch (err) {
        const { action, tabId, tempId, cost, typeSell, iconSpec, isNew, options, itemShopId } = req.body;
        if (action === 'add_item') {
            try {
                const [result] = await pool.execute(
                    `INSERT INTO item_shop (tab_id, temp_id, is_new, cost, icon_spec, type_sell, is_sell, create_time) VALUES (?, ?, ?, ?, ?, ?, 1, NOW())`,
                    [tabId, tempId, isNew || 0, cost, iconSpec !== undefined ? iconSpec : -1, typeSell]
                );
                const insertedId = result.insertId;
                if (insertedId && Array.isArray(options)) {
                    for (let opt of options) {
                        await pool.execute(
                            `INSERT INTO item_shop_option (item_shop_id, option_id, param) VALUES (?, ?, ?)`,
                            [insertedId, opt.id, opt.param]
                        );
                    }
                }
                return res.json({ status: 'success', message: 'Đã thêm vật phẩm vào Shop NPC trong MySQL thành công!' });
            } catch (e) {
                return res.status(500).json({ status: 'error', message: 'Lỗi khi thêm vào MySQL: ' + e.message });
            }
        } else if (action === 'delete_item') {
            try {
                await pool.execute(`DELETE FROM item_shop_option WHERE item_shop_id = ?`, [itemShopId]);
                await pool.execute(`DELETE FROM item_shop WHERE id = ?`, [itemShopId]);
                return res.json({ status: 'success', message: 'Đã xóa vật phẩm khỏi Shop NPC trong MySQL!' });
            } catch (e) {
                return res.status(500).json({ status: 'error', message: 'Lỗi khi xóa khỏi MySQL: ' + e.message });
            }
        }
        return res.status(400).json({ status: 'error', message: 'Hành động không hợp lệ' });
    }
});

app.post('/api/drop-rules', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/drop-rules`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req.body)
        });
        const data = await resp.json();
        return res.status(resp.status).json(data);
    } catch (err) {
        res.status(500).json({ status: 'error', message: 'Không thể kết nối Java Server API. Vui lòng bật server bằng lệnh bash run.sh.' });
    }
});

// --- GIFTCODE ENDPOINTS ---
app.get('/api/admin/giftcodes', async (req, res) => {
    try {
        const [rows] = await pool.execute('SELECT id, code, count_left, datecreate, expired, detail FROM giftcode ORDER BY id DESC');
        return res.json(rows);
    } catch (err) {
        return res.status(500).json({ status: 'error', message: err.message });
    }
});

app.post('/api/admin/giftcodes', async (req, res) => {
    try {
        const { code, countLeft, dateexpired, detail } = req.body;
        if (!code || !code.trim()) {
            return res.status(400).json({ status: 'error', message: 'Vui lòng nhập mã GiftCode' });
        }
        const cleanCode = code.trim();
        const count = countLeft !== undefined ? parseInt(countLeft) : 100;
        const detailJson = JSON.stringify(detail || []);
        const expDate = dateexpired ? new Date(dateexpired) : new Date(Date.now() + 365 * 24 * 60 * 60 * 1000);

        // Check duplicate
        const [exist] = await pool.execute('SELECT id FROM giftcode WHERE code = ?', [cleanCode]);
        if (exist.length > 0) {
            return res.status(400).json({ status: 'error', message: `Mã GiftCode [${cleanCode}] đã tồn tại!` });
        }

        await pool.execute(
            'INSERT INTO giftcode (code, count_left, datecreate, expired, detail) VALUES (?, ?, NOW(), ?, ?)',
            [cleanCode, count, expDate, detailJson]
        );

        // Call Java server reload API
        try {
            await fetch(`${JAVA_API_URL}/api/reload-giftcode`, { method: 'POST' });
        } catch (e) { }

        return res.json({ status: 'success', message: `Đã tạo và kích hoạt GiftCode [${cleanCode}] thành công!` });
    } catch (err) {
        return res.status(500).json({ status: 'error', message: err.message });
    }
});

app.delete('/api/admin/giftcodes/:id', async (req, res) => {
    try {
        const giftId = req.params.id;
        await pool.execute('DELETE FROM giftcode WHERE id = ?', [giftId]);
        try {
            await fetch(`${JAVA_API_URL}/api/reload-giftcode`, { method: 'POST' });
        } catch (e) { }
        return res.json({ status: 'success', message: 'Đã xóa GiftCode thành công!' });
    } catch (err) {
        return res.status(500).json({ status: 'error', message: err.message });
    }
});

app.post('/api/user/use-giftcode', async (req, res) => {
    try {
        const { playerName, code } = req.body;
        if (!playerName || !code) {
            return res.status(400).json({ status: 'error', message: 'Vui lòng nhập mã GiftCode' });
        }

        const resp = await fetch(`${JAVA_API_URL}/api/use-giftcode`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerName, code: code.trim() })
        });
        const data = await resp.json();
        return res.status(resp.status).json(data);
    } catch (err) {
        return res.status(500).json({ status: 'error', message: 'Không thể gửi mã đến Game Server. Hãy chắc chắn Game Server đang chạy.' });
    }
});

app.post('/api/adjust-player-power', async (req, res) => {
    try {
        const resp = await fetch(`${JAVA_API_URL}/api/adjust-player-power`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req.body)
        });
        const data = await resp.json();
        return res.status(resp.status).json(data);
    } catch (err) {
        res.status(500).json({ status: 'error', message: 'Không thể kết nối Java Server API. Vui lòng bật server bằng lệnh bash run.sh.' });
    }
});

// --- FIX DUPLICATE ITEM OPTIONS (Sửa Đồ Lỗi Trùng Chỉ Số) ---
app.post('/api/fix-duplicate-options', async (req, res) => {
    try {
        const columns = ['items_bag', 'items_body', 'items_box'];
        let totalPlayersFixed = 0;
        let totalItemsFixed = 0;

        const [players] = await pool.execute('SELECT id, name, items_bag, items_body, items_box FROM player');

        for (const player of players) {
            let playerModified = false;
            const updates = {};

            for (const col of columns) {
                const raw = player[col];
                if (!raw || raw === '[]' || raw === 'null') continue;

                let itemsArray;
                try { itemsArray = JSON.parse(raw); } catch (e) { continue; }
                if (!Array.isArray(itemsArray)) continue;

                let colModified = false;

                for (let i = 0; i < itemsArray.length; i++) {
                    let slot = itemsArray[i];
                    // Slot can be a string or an array
                    let slotArr;
                    if (typeof slot === 'string') {
                        try { slotArr = JSON.parse(slot); } catch (e) { continue; }
                    } else if (Array.isArray(slot)) {
                        slotArr = slot;
                    } else {
                        continue;
                    }

                    if (!Array.isArray(slotArr) || slotArr.length < 3) continue;

                    const tempId = parseInt(slotArr[0]);
                    if (tempId === -1) continue; // Empty slot

                    // slotArr[2] is options string like '[[optId,param],[optId,param]]'
                    let optionsRaw = slotArr[2];
                    let options;
                    if (typeof optionsRaw === 'string') {
                        try { options = JSON.parse(optionsRaw); } catch (e) { continue; }
                    } else if (Array.isArray(optionsRaw)) {
                        options = optionsRaw;
                    } else {
                        continue;
                    }

                    if (!Array.isArray(options) || options.length <= 1) continue;

                    // Parse each option: can be "[optId,param]" string or [optId,param] array
                    const parsedOpts = [];
                    for (const optRaw of options) {
                        let opt;
                        if (typeof optRaw === 'string') {
                            try { opt = JSON.parse(optRaw); } catch (e) { continue; }
                        } else if (Array.isArray(optRaw)) {
                            opt = optRaw;
                        } else {
                            continue;
                        }
                        if (Array.isArray(opt) && opt.length >= 2) {
                            parsedOpts.push(opt);
                        }
                    }

                    // Deduplicate: keep only the FIRST occurrence of each option ID
                    const seenIds = new Set();
                    const uniqueOpts = [];
                    for (const opt of parsedOpts) {
                        const optId = parseInt(opt[0]);
                        if (!seenIds.has(optId)) {
                            seenIds.add(optId);
                            uniqueOpts.push(opt);
                        }
                    }

                    // If duplicates were found and removed
                    if (uniqueOpts.length < parsedOpts.length) {
                        totalItemsFixed++;
                        colModified = true;

                        // Rebuild options array in original format (array of JSON strings)
                        const newOptions = uniqueOpts.map(o => JSON.stringify(o));
                        slotArr[2] = JSON.stringify(newOptions);

                        // Write back slot
                        if (typeof slot === 'string') {
                            itemsArray[i] = JSON.stringify(slotArr);
                        } else {
                            itemsArray[i] = slotArr;
                        }
                    }
                }

                if (colModified) {
                    playerModified = true;
                    updates[col] = JSON.stringify(itemsArray);
                }
            }

            if (playerModified) {
                totalPlayersFixed++;
                const setClauses = Object.keys(updates).map(k => `${k} = ?`).join(', ');
                const values = [...Object.values(updates), player.id];
                await pool.execute(`UPDATE player SET ${setClauses} WHERE id = ?`, values);
            }
        }

        res.json({
            status: 'success',
            message: `✅ Đã quét ${players.length} nhân vật. Sửa ${totalItemsFixed} món đồ bị trùng chỉ số trên ${totalPlayersFixed} nhân vật!`
        });
    } catch (err) {
        console.error('Fix duplicate options error:', err);
        res.status(500).json({ status: 'error', message: 'Lỗi khi sửa đồ: ' + err.message });
    }
});

app.listen(PORT, () => {
    console.log(`=================================================`);
    console.log(`🔥 WEB ADMIN PANEL NRO READY!`);
    console.log(`🌐 Truy cập Admin Web tại: http://localhost:${PORT}`);
    console.log(`=================================================`);
});

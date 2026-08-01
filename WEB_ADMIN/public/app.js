// Global Application State
let itemTemplates = [];
let mapTemplates = [];
let optionTemplates = [];
let playersList = [];
let accountsList = [];

// --- AUTH & SWITCHER ---
function switchAuthTab(type) {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const tabLogin = document.getElementById('auth-tab-login');
    const tabRegister = document.getElementById('auth-tab-register');

    if (type === 'register') {
        if (loginForm) loginForm.style.display = 'none';
        if (registerForm) registerForm.style.display = 'block';
        if (tabLogin) tabLogin.classList.remove('active');
        if (tabRegister) tabRegister.classList.add('active');
    } else {
        if (registerForm) registerForm.style.display = 'none';
        if (loginForm) loginForm.style.display = 'block';
        if (tabRegister) tabRegister.classList.remove('active');
        if (tabLogin) tabLogin.classList.add('active');
    }
}

function setupRegisterForm() {
    const form = document.getElementById('register-form');
    if (!form) return;
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('reg-user').value.trim();
        const emailEl = document.getElementById('reg-email');
        const email = emailEl ? emailEl.value.trim() : '';
        const password = document.getElementById('reg-pass').value.trim();
        const confirmPass = document.getElementById('reg-pass-confirm').value.trim();

        if (password !== confirmPass) {
            return showToast('Mật khẩu xác nhận không khớp!', 'error');
        }

        try {
            const resp = await fetch('/api/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
            });
            const data = await resp.json();

            if (data.status === 'success') {
                showToast(data.message, 'success');
                document.getElementById('admin-user').value = username;
                document.getElementById('admin-pass').value = password;
                switchAuthTab('login');
            } else {
                showToast(data.message || 'Đăng ký thất bại', 'error');
            }
        } catch (err) {
            showToast('Lỗi khi đăng ký tài khoản', 'error');
        }
    });
}
let cartItems = []; // Array of items to be granted to player
let dropRulesList = [];
let npcShopsList = [];
let newShopItemOptions = [];
let currentTab = 'grant';

// Popular default NRO option templates for quick selection
const POPULAR_OPTIONS = [
    { id: 0, name: 'Sức đánh' },
    { id: 6, name: 'HP' },
    { id: 7, name: 'KI' },
    { id: 14, name: 'Chí mạng (%)' },
    { id: 50, name: 'Sức đánh (%)' },
    { id: 77, name: 'HP (%)' },
    { id: 103, name: 'KI (%)' },
    { id: 30, name: 'Không thể giao dịch' },
    { id: 93, name: 'Hạn sử dụng (ngày)' }
];

document.addEventListener('DOMContentLoaded', () => {
    checkLoginState();
    setupLoginForm();
    setupRegisterForm();
    setupOutsideClickListener();
    loadStats();
});

// --- LOGIN & AUTH ---
function clearLoginInputs() {
    const u = document.getElementById('admin-user');
    const p = document.getElementById('admin-pass');
    if (u) u.value = '';
    if (p) p.value = '';
}

function updatePortalUserWidget() {
    const unloggedState = document.getElementById('widget-unlogged-state');
    const loggedState = document.getElementById('widget-logged-state');
    const adminBtn = document.getElementById('admin-panel-access-btn');
    const authHeaderBtn = document.getElementById('public-auth-header-btn');

    if (!currentLoggedUser) {
        if (unloggedState) unloggedState.style.display = 'block';
        if (loggedState) loggedState.style.display = 'none';
        if (adminBtn) adminBtn.style.display = 'none';
        if (authHeaderBtn) {
            authHeaderBtn.innerHTML = `
                <button type="button" class="btn-primary" onclick="openLoginModal()" style="padding: 8px 18px; font-size: 13px; font-weight: 800;">
                    <i class="fa-solid fa-right-to-bracket"></i> ĐĂNG NHẬP / ĐĂNG KÝ
                </button>`;
        }
    } else {
        if (unloggedState) unloggedState.style.display = 'none';
        if (loggedState) loggedState.style.display = 'block';

        if (authHeaderBtn) {
            authHeaderBtn.innerHTML = `
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="font-weight: 800; color: var(--teamobi-orange-dark); font-size: 13px;"><i class="fa-solid fa-user"></i> ${escapeHtml(currentLoggedUser.username)}</span>
                    <button type="button" class="btn-secondary" onclick="logoutAdmin()" style="padding: 6px 12px; font-size: 12px; color: var(--red);">Đăng Xuất</button>
                </div>`;
        }

        const nameEl = document.getElementById('portal-welcome-name');
        if (nameEl) nameEl.innerText = currentLoggedUser.username;

        const subEl = document.getElementById('portal-sub-info');
        if (subEl) subEl.innerText = currentLoggedUser.admin >= 1 ? 'QUẢN TRỊ VIÊN (ADMIN)' : 'Thành viên Game';

        if (currentLoggedUser.admin >= 1) {
            if (adminBtn) adminBtn.style.display = 'block';
        } else {
            if (adminBtn) adminBtn.style.display = 'none';
        }

        const noCharAlert = document.getElementById('portal-no-char-alert');
        const hasCharCard = document.getElementById('portal-has-char-card');

        if (!currentLoggedUser.hasPlayer || !currentLoggedUser.player) {
            if (noCharAlert) noCharAlert.style.display = 'block';
            if (hasCharCard) hasCharCard.style.display = 'none';
        } else {
            if (noCharAlert) noCharAlert.style.display = 'none';
            if (hasCharCard) hasCharCard.style.display = 'block';

            const p = currentLoggedUser.player;
            const cName = document.getElementById('portal-char-name');
            const cGender = document.getElementById('portal-char-gender');
            const cId = document.getElementById('portal-char-id');
            const cAvatar = document.getElementById('portal-avatar-img');

            if (cName) cName.innerText = p.name || '-';
            if (cGender) cGender.innerText = p.gender !== undefined ? (p.gender === 0 ? 'Trái Đất' : p.gender === 1 ? 'Namếc' : 'Xayda') : '-';
            if (cId) cId.innerText = `#${p.id || 0}`;
            if (cAvatar) {
                const imgUrl = (p.avatarUrl && !p.avatarUrl.includes('64.png') && !p.avatarUrl.includes('28.png')) ? p.avatarUrl : (p.avatarId ? `/icons/${p.avatarId}.png` : ((p.gender === 1 || p.gender === 'Namếc') ? '/icons/523.png' : (p.gender === 2 || p.gender === 'Xayda') ? '/icons/519.png' : '/icons/521.png'));
                cAvatar.src = imgUrl;
            }
        }

        const navUserDisplay = document.getElementById('nav-username-display');
        if (navUserDisplay) navUserDisplay.innerText = currentLoggedUser.username || 'Admin';

        const adminSidebarAvatar = document.getElementById('admin-sidebar-avatar-img');
        if (adminSidebarAvatar) {
            let adminAvatarSrc = '/icons/521.png';
            if (currentLoggedUser.player) {
                const p = currentLoggedUser.player;
                adminAvatarSrc = (p.avatarUrl && !p.avatarUrl.includes('64.png') && !p.avatarUrl.includes('28.png')) ? p.avatarUrl : (p.avatarId ? `/icons/${p.avatarId}.png` : ((p.gender === 1 || p.gender === 'Namếc') ? '/icons/523.png' : (p.gender === 2 || p.gender === 'Xayda') ? '/icons/519.png' : '/icons/521.png'));
            }
            adminSidebarAvatar.src = adminAvatarSrc;
        }
    }
}

function showPublicHome() {
    const appLayout = document.querySelector('.app-layout');
    if (appLayout) appLayout.style.setProperty('display', 'none', 'important');

    const publicHome = document.getElementById('public-home-view');
    if (publicHome) publicHome.style.setProperty('display', 'block', 'important');

    closeLoginModal();
    updatePortalUserWidget();
}

async function initAdminData() {
    try {
        if (typeof loadStats === 'function') loadStats();
        if (typeof loadItemTemplates === 'function') await loadItemTemplates();
        if (typeof loadMapTemplates === 'function') await loadMapTemplates();
        if (typeof loadOptionTemplates === 'function') await loadOptionTemplates();
        if (typeof loadPlayers === 'function') await loadPlayers();
    } catch (e) {
        console.error('Error initializing admin data:', e);
    }
}

function showAdminPanel() {
    if (!currentLoggedUser || currentLoggedUser.admin < 1) {
        return showToast('Bạn cần quyền Admin để truy cập Admin Panel!', 'error');
    }

    if (window.location.hash) {
        history.pushState('', document.title, window.location.pathname + window.location.search);
    }

    const publicHome = document.getElementById('public-home-view');
    if (publicHome) publicHome.style.setProperty('display', 'none', 'important');

    const appLayout = document.querySelector('.app-layout');
    if (appLayout) appLayout.style.setProperty('display', 'flex', 'important');

    const sidebar = document.querySelector('.sidebar');
    if (sidebar) sidebar.style.setProperty('display', 'flex', 'important');

    window.scrollTo(0, 0);

    switchTab(currentTab || 'grant');
    initAdminData();
}

function openLoginModal() {
    const loginScreen = document.getElementById('login-screen');
    if (loginScreen) {
        loginScreen.style.setProperty('display', 'flex', 'important');
    }
}

function closeLoginModal() {
    const loginScreen = document.getElementById('login-screen');
    if (loginScreen) {
        loginScreen.style.setProperty('display', 'none', 'important');
    }
}

function checkLoginState() {
    const saved = sessionStorage.getItem('nro_logged_user');
    if (saved) {
        try {
            currentLoggedUser = JSON.parse(saved);
            if (currentLoggedUser && currentLoggedUser.player) {
                if (!currentLoggedUser.player.avatarUrl || currentLoggedUser.player.avatarUrl.includes('64.png') || currentLoggedUser.player.avatarUrl.includes('28.png')) {
                    const g = currentLoggedUser.player.gender;
                    currentLoggedUser.player.avatarUrl = (g === 1 || g === 'Namếc') ? '/icons/523.png' : (g === 2 || g === 'Xayda') ? '/icons/519.png' : '/icons/521.png';
                    sessionStorage.setItem('nro_logged_user', JSON.stringify(currentLoggedUser));
                }
            }
            if (currentLoggedUser && currentLoggedUser.admin >= 1) {
                closeLoginModal();
                showAdminPanel();
                return;
            }
        } catch (e) { }
    }
    showPublicHome();
}

async function executeLogin(username, password) {
    if (!username || !password) {
        return showToast('Vui lòng nhập đầy đủ tên tài khoản và mật khẩu!', 'error');
    }

    try {
        const resp = await fetch('/api/admin/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await resp.json();

        if (data.success) {
            currentLoggedUser = data.user;
            sessionStorage.setItem('nro_logged_user', JSON.stringify(data.user));
            closeLoginModal();
            showToast(data.message, 'success');

            if (data.user.admin >= 1) {
                showAdminPanel();
            } else {
                showPublicHome();
            }
        } else {
            showToast(data.message || 'Đăng nhập thất bại', 'error');
        }
    } catch (err) {
        showToast('Không thể kết nối đến Web Server', 'error');
    }
}

async function handleLoginSubmit(e) {
    if (e) e.preventDefault();
    const u = document.getElementById('admin-user');
    const p = document.getElementById('admin-pass');
    const username = u ? u.value.trim() : '';
    const password = p ? p.value.trim() : '';
    await executeLogin(username, password);
}

async function handleQuickInlineLogin(e) {
    if (e) e.preventDefault();
    const u = document.getElementById('inline-user');
    const p = document.getElementById('inline-pass');
    const username = u ? u.value.trim() : '';
    const password = p ? p.value.trim() : '';
    await executeLogin(username, password);
}

function setupLoginForm() {
    const form = document.getElementById('login-form');
    if (!form) return;
    form.addEventListener('submit', handleLoginSubmit);
}

function logoutAdmin() {
    sessionStorage.removeItem('nro_logged_user');
    currentLoggedUser = null;
    clearLoginInputs();
    showPublicHome();
}

async function buyGoldForPlayer(quantity) {
    if (!currentLoggedUser) {
        showToast('Vui lòng đăng nhập lại!', 'error');
        return;
    }
    if (!currentLoggedUser.hasPlayer) {
        customConfirm(
            'CHƯA CÓ NHÂN VẬT',
            'Tài khoản của bạn <strong style="color: var(--red);">chưa tạo nhân vật</strong> trong game.<br>Vui lòng mở Game Ngọc Rồng trên máy, đăng nhập và <strong>tạo nhân vật mới</strong> trước khi mua vàng nhé!',
            null
        );
        return;
    }

    try {
        const resp = await fetch('/api/user/buy-gold', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountId: currentLoggedUser.id, quantity })
        });
        const data = await resp.json();
        showToast(data.message, data.status === 'success' ? 'success' : 'error');
    } catch (e) {
        showToast('Lỗi khi gửi yêu cầu mua vàng', 'error');
    }
}

// --- PUBLIC NAVBAR SWITCHER ---
function selectPublicTab(tabName, event) {
    if (event) event.preventDefault();
    const navBtns = document.querySelectorAll('.teamobi-nav-bar .t-nav-btn');
    navBtns.forEach(btn => btn.classList.remove('active'));

    const targetBtn = document.getElementById('pub-nav-' + tabName);
    if (targetBtn) targetBtn.classList.add('active');

    if (tabName === 'home') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    } else if (tabName === 'gioi-thieu') {
        const el = document.getElementById('gioi-thieu-sec');
        if (el) el.scrollIntoView({ behavior: 'smooth' });
    } else if (tabName === 'huong-dan') {
        const el = document.getElementById('huong-dan-sec');
        if (el) el.scrollIntoView({ behavior: 'smooth' });
    } else if (tabName === 'nap-vang') {
        const el = document.getElementById('giftcode-sec');
        if (el) el.scrollIntoView({ behavior: 'smooth' });
    } else if (tabName === 'dien-dan') {
        const el = document.getElementById('forum-sec');
        if (el) el.scrollIntoView({ behavior: 'smooth' });
    }
}

// --- ADMIN SIDEBAR MENU SWITCHER ---
function selectAdminTab(tabName) {
    currentTab = tabName;
    if (typeof closeMobileSidebar === 'function') closeMobileSidebar();

    // 1. Hide all tab contents
    const allTabs = document.getElementsByClassName('tab-content');
    for (let i = 0; i < allTabs.length; i++) {
        allTabs[i].style.setProperty('display', 'none', 'important');
    }

    // 2. Show target tab content
    const targetTab = document.getElementById('tab-' + tabName);
    if (targetTab) {
        targetTab.style.setProperty('display', 'block', 'important');
    }

    // 3. Update active state on sidebar menu items
    const allBtns = document.getElementsByClassName('admin-nav-item');
    for (let i = 0; i < allBtns.length; i++) {
        allBtns[i].classList.remove('active-nav-item');
    }

    const activeBtn = document.getElementById('menu-btn-' + tabName);
    if (activeBtn) {
        activeBtn.classList.add('active-nav-item');
    }

    // 4. Tab-specific data reload triggers
    try {
        if (tabName === 'accounts' && typeof loadAccountData === 'function') loadAccountData();
        else if ((tabName === 'players' || tabName === 'event-points') && typeof loadPlayers === 'function') loadPlayers();
        else if (tabName === 'bots' && typeof loadBotsList === 'function') loadBotsList();
        else if (tabName === 'bosses' && typeof loadBosses === 'function') loadBosses();
        else if (tabName === 'giftcode' && typeof loadAdminGiftcodes === 'function') loadAdminGiftcodes();
        else if (tabName === 'events' && typeof loadServerEvents === 'function') loadServerEvents();
        else if (tabName === 'drops' && typeof loadDropRules === 'function') loadDropRules();
        else if (tabName === 'shops' && typeof loadNpcShops === 'function') loadNpcShops();
    } catch (e) {
        console.error('Error reloading tab data:', e);
    }
}

function toggleMobileSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    if (sidebar) {
        sidebar.classList.toggle('open');
        if (backdrop) {
            backdrop.classList.toggle('active', sidebar.classList.contains('open'));
        }
    }
}

function closeMobileSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    if (sidebar) sidebar.classList.remove('open');
    if (backdrop) backdrop.classList.remove('active');
}

function switchTab(tabName) {
    selectAdminTab(tabName);
}

async function loadStats() {
    try {
        const resp = await fetch('/api/admin/stats');
        if (resp.ok) {
            const data = await resp.json();
            const elAcc = document.getElementById('stat-accounts');
            const elPl = document.getElementById('stat-players');
            const elOn = document.getElementById('stat-online');
            const elPubOn = document.getElementById('public-stat-online');

            if (elAcc) elAcc.innerText = (data.totalAccounts || 0).toLocaleString('vi-VN');
            if (elPl) elPl.innerText = (data.totalPlayers || 0).toLocaleString('vi-VN');
            if (elOn) elOn.innerText = (data.onlinePlayers || 0).toLocaleString('vi-VN');
            if (elPubOn) elPubOn.innerText = (data.onlinePlayers || 0).toLocaleString('vi-VN');
        }
    } catch (e) {
        console.error('Failed to load stats', e);
    }
}

async function loadItemTemplates() {
    try {
        const resp = await fetch('/api/item-templates');
        if (resp.ok) {
            const data = await resp.json();
            if (Array.isArray(data) && data.length > 0) {
                itemTemplates = data;
                document.getElementById('item-count-badge').innerText = itemTemplates.length + ' Món';
                renderItemCatalog(itemTemplates.slice(0, 120));
                return;
            }
        }
    } catch (e) {
        console.error('Failed to load item templates', e);
    }
    document.getElementById('item-count-badge').innerText = '0 Món (Đang kết nối DB)';
}

async function loadMapTemplates() {
    try {
        const resp = await fetch('/api/map-templates');
        if (resp.ok) {
            const data = await resp.json();
            if (Array.isArray(data)) {
                mapTemplates = data;
            }
        }
    } catch (e) {
        console.error('Failed to load map templates', e);
    }
}

async function loadOptionTemplates() {
    try {
        const resp = await fetch('/api/option-templates');
        if (resp.ok) {
            const data = await resp.json();
            if (Array.isArray(data) && data.length > 0) {
                optionTemplates = data;
                return;
            }
        }
    } catch (e) { }
    optionTemplates = POPULAR_OPTIONS;
}

async function loadPlayers() {
    try {
        const resp = await fetch('/api/players');
        if (resp.ok) {
            playersList = await resp.json();
            renderPlayerTable();
            if (typeof renderTabEventPointTable === 'function') renderTabEventPointTable();
            checkTargetPlayerStatus();
        }
    } catch (e) {
        console.error('Failed to load players', e);
    }
}

// --- NPC SHOP MANAGEMENT STUDIO ---
async function loadNpcShops() {
    try {
        const resp = await fetch('/api/npc-shops');
        if (resp.ok) {
            npcShopsList = await resp.json();
            populateNpcShopSelect();
        }
    } catch (e) {
        console.error('Failed to load NPC shops', e);
    }
}

function populateNpcShopSelect() {
    const npcSelect = document.getElementById('shop-npc-select');
    if (!npcSelect) return;

    npcSelect.innerHTML = '';
    if (!npcShopsList || npcShopsList.length === 0) {
        npcSelect.innerHTML = '<option value="">Không tìm thấy Shop NPC</option>';
        return;
    }

    npcShopsList.forEach(shop => {
        const opt = document.createElement('option');
        opt.value = shop.shopId;
        opt.innerText = `[NPC #${shop.npcId}] ${shop.npcName} (${shop.tagName})`;
        npcSelect.appendChild(opt);
    });

    handleNpcShopSelectChange();
}

function handleNpcShopSelectChange() {
    const shopId = parseInt(document.getElementById('shop-npc-select').value);
    const tabSelect = document.getElementById('shop-tab-select');
    tabSelect.innerHTML = '';

    const shopObj = npcShopsList.find(s => s.shopId === shopId);
    if (!shopObj || !shopObj.tabs || shopObj.tabs.length === 0) {
        tabSelect.innerHTML = '<option value="">Không có tab shop nào</option>';
        renderSelectedShopItemsTable();
        return;
    }

    shopObj.tabs.forEach(tab => {
        const opt = document.createElement('option');
        opt.value = tab.tabId;
        opt.innerText = `Tab #${tab.tabId}: ${tab.name}`;
        tabSelect.appendChild(opt);
    });

    renderSelectedShopItemsTable();
}

function renderSelectedShopItemsTable() {
    const shopId = parseInt(document.getElementById('shop-npc-select').value);
    const tabId = parseInt(document.getElementById('shop-tab-select').value);
    const tbody = document.getElementById('shop-items-table-body');
    const badge = document.getElementById('shop-items-count-badge');
    tbody.innerHTML = '';

    const shopObj = npcShopsList.find(s => s.shopId === shopId);
    if (!shopObj) {
        badge.innerText = '0 Món';
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 20px;">Vui lòng chọn Shop NPC</td></tr>';
        return;
    }

    const tabObj = shopObj.tabs ? shopObj.tabs.find(t => t.tabId === tabId) : null;
    if (!tabObj || !tabObj.items || tabObj.items.length === 0) {
        badge.innerText = '0 Món';
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 20px;">Tab Shop này chưa có món đồ nào</td></tr>';
        return;
    }

    badge.innerText = tabObj.items.length + ' Món Đang Bán';

    tabObj.items.forEach(item => {
        const tr = document.createElement('tr');
        tr.style.borderBottom = '1px solid rgba(255, 255, 255, 0.05)';

        let currencyBadge = '';
        if (item.iconSpec === 4028) {
            currencyBadge = '<span style="color: var(--gold); font-weight: 700;">🪙 Thỏi Vàng</span>';
        } else if (item.iconSpec >= 419 && item.iconSpec <= 425) {
            const star = item.iconSpec - 418;
            currencyBadge = `<span style="color: var(--gold); font-weight: 700;">⭐ Ngọc Rồng ${star} Sao</span>`;
        } else if (item.iconSpec > 0) {
            currencyBadge = `<span style="color: var(--cyan); font-weight: 700;">🎒 Item Icon #${item.iconSpec}</span>`;
        } else if (item.typeSell === 0) {
            currencyBadge = '<span style="color: var(--gold); font-weight: 700;">💰 Vàng</span>';
        } else if (item.typeSell === 1) {
            currencyBadge = '<span style="color: var(--cyan); font-weight: 700;">💎 Ngọc Xanh</span>';
        } else if (item.typeSell === 3) {
            currencyBadge = '<span style="color: #ff4757; font-weight: 700;">🔴 Hồng Ngọc</span>';
        } else {
            currencyBadge = '<span style="color: #b537f2; font-weight: 700;">🎟️ Coupon</span>';
        }

        const newTag = item.isNew ? '<span class="badge-online" style="font-size: 10px; background: rgba(0, 255, 135, 0.2); color: #00ff87;">NEW</span>' : '-';

        let optsText = '';
        if (item.options && item.options.length > 0) {
            optsText = item.options.map(o => {
                const optT = (optionTemplates || POPULAR_OPTIONS).find(pt => pt.id === o.id);
                const optName = optT ? optT.name : `Opt #${o.id}`;
                return `<span style="font-size: 11px; background: rgba(255, 255, 255, 0.05); padding: 2px 6px; border-radius: 4px; display: inline-block; margin: 2px;">#${o.id} ${escapeHtml(optName)}: +${o.param}</span>`;
            }).join(' ');
        } else {
            optsText = '<span style="color: var(--text-muted); font-size: 12px;">Không có chỉ số</span>';
        }

        const iconId = item.iconId !== undefined && item.iconId !== null ? item.iconId : item.tempId;

        tr.innerHTML = `
            <td data-label="Vật Phẩm" style="padding: 12px; font-weight: 700; color: var(--text-main);">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <img src="/icons/${iconId}.png" onerror="this.onerror=null; this.src='/icons_x1/${iconId}.png';" style="width: 24px; height: 24px;" alt="">
                    <span>#${item.tempId} - ${escapeHtml(item.name)}</span>
                </div>
            </td>
            <td data-label="Giá Bán" style="padding: 12px; color: var(--gold); font-weight: 800; font-size: 15px;">${item.cost.toLocaleString('vi-VN')}</td>
            <td data-label="Loại Tiền" style="padding: 12px;">${currencyBadge}</td>
            <td data-label="Chỉ Số Option" style="padding: 12px;">${optsText}</td>
            <td data-label="Hàng Mới" style="padding: 12px;">${newTag}</td>
            <td data-label="Thao Tác" style="padding: 12px;">
                <button type="button" class="btn-danger" style="padding: 4px 10px; font-size: 11px;" onclick="deleteNpcShopItem(${item.id})">
                    <i class="fa-solid fa-trash"></i> Xóa
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function handleShopItemSearchInput() {
    const input = document.getElementById('shop-item-search');
    const val = input.value.trim().toLowerCase();
    const dropdown = document.getElementById('shop-item-suggestions');
    const hiddenId = document.getElementById('shop-item-id');

    if (itemTemplates.length === 0) {
        await loadItemTemplates();
    }

    let matches = itemTemplates;
    if (val) {
        if (!isNaN(val)) {
            hiddenId.value = val;
            matches = itemTemplates.filter(i => i.id.toString().includes(val) || (i.name && i.name.toLowerCase().includes(val)));
        } else {
            matches = itemTemplates.filter(i => i.name && i.name.toLowerCase().includes(val));
        }
    }

    if (!matches || matches.length === 0) {
        dropdown.style.display = 'none';
        return;
    }

    dropdown.innerHTML = '';
    matches.slice(0, 15).forEach(i => {
        const iconId = (i.iconID !== undefined && i.iconID !== null) ? i.iconID : i.icon_id || i.id;
        const item = document.createElement('div');
        item.className = 'suggestion-item';
        item.innerHTML = `
            <span style="display: flex; align-items: center; gap: 8px;">
                <img src="/icons/${iconId}.png" onerror="this.onerror=null; this.src='/icons_x1/${iconId}.png';" style="width: 20px; height: 20px;" alt="">
                #${i.id} - <strong>${escapeHtml(i.name)}</strong>
            </span>
        `;
        item.onclick = () => {
            input.value = `#${i.id} - ${i.name}`;
            hiddenId.value = i.id;
            dropdown.style.display = 'none';
        };
        dropdown.appendChild(item);
    });

    dropdown.style.display = 'block';
}

function addShopOptionRow() {
    newShopItemOptions.unshift({ id: 0, param: 10 });
    renderShopOptionsList();
}

function removeShopOptionRow(idx) {
    if (idx >= 0 && idx < newShopItemOptions.length) {
        newShopItemOptions.splice(idx, 1);
        renderShopOptionsList();
    }
}

function updateShopOptionValue(idx, field, val) {
    if (newShopItemOptions[idx]) {
        newShopItemOptions[idx][field] = parseInt(val) || 0;
    }
}

function renderShopOptionsList() {
    const container = document.getElementById('shop-item-options-list');
    container.innerHTML = '';

    const listToUse = (optionTemplates && optionTemplates.length > 0) ? optionTemplates : POPULAR_OPTIONS;

    newShopItemOptions.forEach((opt, idx) => {
        let selectOptions = '';
        listToUse.forEach(o => {
            const sel = o.id === opt.id ? 'selected' : '';
            selectOptions += `<option value="${o.id}" ${sel}>${o.id} - ${escapeHtml(o.name)}</option>`;
        });

        const row = document.createElement('div');
        row.className = 'option-row';
        row.style.marginBottom = '8px';
        row.innerHTML = `
            <div style="flex: 2; display: flex; flex-direction: column; gap: 4px;">
                <input type="text" class="input-field" placeholder="🔍 Gõ tên hoặc ID để lọc..." style="padding: 6px 10px; font-size: 12px; background: #ffffff; color: var(--text-main); border: 1px solid #cbd5e1;" oninput="filterOptionSelect(this, null, null, ${idx})" />
                <select class="select-field" style="width: 100%; font-size: 12px;" onchange="updateShopOptionValue(${idx}, 'id', this.value)">
                    ${selectOptions}
                </select>
            </div>
            <input type="number" class="input-field" value="${opt.param}" style="flex: 1;" placeholder="Param..." oninput="updateShopOptionValue(${idx}, 'param', this.value)" required>
            <button type="button" class="btn-danger" style="align-self: flex-end; padding: 8px;" onclick="removeShopOptionRow(${idx})">
                <i class="fa-solid fa-trash"></i>
            </button>
        `;
        container.appendChild(row);
    });
}

async function addNpcShopItem(e) {
    e.preventDefault();

    const tabId = parseInt(document.getElementById('shop-tab-select').value);
    const tempId = parseInt(document.getElementById('shop-item-id').value);
    const cost = parseInt(document.getElementById('shop-item-cost').value) || 0;
    const currencyVal = document.getElementById('shop-currency-select').value;
    const parts = currencyVal.split('|');
    const typeSell = parseInt(parts[0]) || 0;
    const iconSpec = parts.length > 1 ? parseInt(parts[1]) : -1;
    const isNew = document.getElementById('shop-item-isnew').checked ? 1 : 0;

    if (isNaN(tabId) || isNaN(tempId)) {
        showToast('Vui lòng chọn hoặc nhập ID Vật Phẩm!', 'error');
        return;
    }

    try {
        const resp = await fetch('/api/npc-shops', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                action: 'add_item',
                tabId: tabId,
                tempId: tempId,
                cost: cost,
                typeSell: typeSell,
                iconSpec: iconSpec,
                isNew: isNew,
                options: newShopItemOptions
            })
        });

        const result = await resp.json();
        if (resp.ok && result.status === 'success') {
            showToast(result.message, 'success');
            document.getElementById('shop-item-search').value = '';
            document.getElementById('shop-item-id').value = '';
            newShopItemOptions = [];
            renderShopOptionsList();
            await loadNpcShops();
        } else {
            showToast(result.message || 'Thêm vật phẩm thất bại', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối API Server', 'error');
    }
}

async function deleteNpcShopItem(itemShopId) {
    try {
        const resp = await fetch('/api/npc-shops', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'delete_item', itemShopId: itemShopId })
        });
        const result = await resp.json();
        if (resp.ok && result.status === 'success') {
            showToast(result.message, 'success');
            await loadNpcShops();
        } else {
            showToast(result.message || 'Xóa thất bại', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối API Server', 'error');
    }
}

// --- ITEM DROP RULES MANAGEMENT & MAP SUGGESTIONS ---
async function loadDropRules() {
    try {
        const resp = await fetch('/api/drop-rules');
        if (resp.ok) {
            const data = await resp.json();
            if (data) {
                dropRulesList = data.rules || [];
                renderDropRulesTable();
            }
        }
    } catch (e) {
        console.error('Failed to load drop rules', e);
    }
}

async function handleMapSearchInput() {
    const input = document.getElementById('rule-map-search');
    const val = input.value.trim().toLowerCase();
    const dropdown = document.getElementById('map-suggestions');
    const hiddenId = document.getElementById('rule-map-id');

    if (mapTemplates.length === 0) {
        await loadMapTemplates();
    }

    let matches = mapTemplates;
    if (val && val !== '-1' && !val.includes('tất cả')) {
        if (!isNaN(val)) {
            hiddenId.value = val;
            matches = mapTemplates.filter(m => m.id.toString().includes(val) || (m.name && m.name.toLowerCase().includes(val)));
        } else {
            matches = mapTemplates.filter(m => m.name && m.name.toLowerCase().includes(val));
        }
    }

    dropdown.innerHTML = '';

    // Add "All Maps" option at top
    const allItem = document.createElement('div');
    allItem.className = 'suggestion-item';
    allItem.innerHTML = '<span><i class="fa-solid fa-earth-americas" style="color: var(--gold);"></i> <strong>TẤT CẢ MAP (-1)</strong></span>';
    allItem.onclick = () => {
        input.value = 'TẤT CẢ MAP (-1)';
        hiddenId.value = '-1';
        dropdown.style.display = 'none';
    };
    dropdown.appendChild(allItem);

    if (matches && matches.length > 0) {
        matches.slice(0, 15).forEach(m => {
            const item = document.createElement('div');
            item.className = 'suggestion-item';
            item.innerHTML = `<span><i class="fa-solid fa-map-location-dot" style="color: var(--cyan);"></i> Map #${m.id} - <strong>${escapeHtml(m.name)}</strong></span>`;
            item.onclick = () => {
                input.value = `Map #${m.id} - ${m.name}`;
                hiddenId.value = m.id;
                dropdown.style.display = 'none';
            };
            dropdown.appendChild(item);
        });
    }

    dropdown.style.display = 'block';
}

async function handleRuleItemSearchInput() {
    const input = document.getElementById('rule-item-search');
    const val = input.value.trim().toLowerCase();
    const dropdown = document.getElementById('rule-item-suggestions');
    const hiddenId = document.getElementById('rule-item-id');

    if (itemTemplates.length === 0) {
        await loadItemTemplates();
    }

    let matches = itemTemplates;
    if (val) {
        if (!isNaN(val)) {
            hiddenId.value = val;
            matches = itemTemplates.filter(i => i.id.toString().includes(val) || (i.name && i.name.toLowerCase().includes(val)));
        } else {
            matches = itemTemplates.filter(i => i.name && i.name.toLowerCase().includes(val));
        }
    }

    if (!matches || matches.length === 0) {
        dropdown.style.display = 'none';
        return;
    }

    dropdown.innerHTML = '';
    matches.slice(0, 15).forEach(i => {
        const iconId = (i.iconID !== undefined && i.iconID !== null) ? i.iconID : i.icon_id || i.id;
        const item = document.createElement('div');
        item.className = 'suggestion-item';
        item.innerHTML = `
            <span style="display: flex; align-items: center; gap: 8px;">
                <img src="/icons/${iconId}.png" onerror="this.onerror=null; this.src='/icons_x1/${iconId}.png';" style="width: 20px; height: 20px;" alt="">
                #${i.id} - <strong>${escapeHtml(i.name)}</strong>
            </span>
        `;
        item.onclick = () => {
            input.value = `#${i.id} - ${i.name}`;
            hiddenId.value = i.id;
            dropdown.style.display = 'none';
        };
        dropdown.appendChild(item);
    });

    dropdown.style.display = 'block';
}

function renderDropRulesTable() {
    const tbody = document.getElementById('drop-rules-table-body');
    tbody.innerHTML = '';

    if (!dropRulesList || dropRulesList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 20px;">Chưa có quy tắc rơi đồ nào được cài đặt</td></tr>';
        return;
    }

    dropRulesList.forEach(rule => {
        const tr = document.createElement('tr');
        tr.style.borderBottom = '1px solid rgba(255, 255, 255, 0.05)';

        const itemObj = itemTemplates.find(i => i.id === rule.itemId);
        const itemName = itemObj ? itemObj.name : `Vật phẩm #${rule.itemId}`;

        let mapText = '';
        if (rule.mapId === -1) {
            mapText = '<span style="color: var(--gold); font-weight: 700;"><i class="fa-solid fa-earth-americas"></i> TẤT CẢ MAP (-1)</span>';
        } else {
            const mapObj = mapTemplates.find(m => m.id === rule.mapId);
            const mapName = mapObj ? mapObj.name : `Map #${rule.mapId}`;
            mapText = `<span style="color: var(--cyan); font-weight: 700;"><i class="fa-solid fa-map-location-dot"></i> #${rule.mapId} - ${escapeHtml(mapName)}</span>`;
        }

        const statusBadge = rule.active
            ? '<span class="badge-online" style="cursor: pointer;" onclick="toggleDropRule(' + rule.id + ')">ĐANG BẬT</span>'
            : '<span class="badge-offline" style="cursor: pointer;" onclick="toggleDropRule(' + rule.id + ')">ĐÃ TẮT</span>';

        tr.innerHTML = `
            <td data-label="Bản Đồ" style="padding: 12px;">${mapText}</td>
            <td data-label="Vật Phẩm" style="padding: 12px; font-weight: 700; color: var(--text-main);"><i class="fa-solid fa-box-open" style="color: var(--gold);"></i> #${rule.itemId} - ${escapeHtml(itemName)}</td>
            <td data-label="Số Lượng" style="padding: 12px; color: var(--gold); font-weight: 700;">x${rule.quantity}</td>
            <td data-label="Tỷ Lệ" style="padding: 12px; color: var(--cyan); font-weight: 800;">${rule.ratePercent}%</td>
            <td data-label="Trạng Thái" style="padding: 12px;">${statusBadge}</td>
            <td data-label="Thao Tác" style="padding: 12px;">
                <button type="button" class="btn-danger" style="padding: 4px 10px; font-size: 11px;" onclick="deleteDropRule(${rule.id})">
                    <i class="fa-solid fa-trash"></i> Xóa
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function addDropRule(e) {
    e.preventDefault();
    const mapId = parseInt(document.getElementById('rule-map-id').value);
    const itemId = parseInt(document.getElementById('rule-item-id').value);
    const quantity = parseInt(document.getElementById('rule-quantity').value) || 1;
    const ratePercent = parseInt(document.getElementById('rule-rate-percent').value) || 5;

    if (isNaN(mapId) || isNaN(itemId)) {
        showToast('Vui lòng chọn hoặc nhập ID Map và ID Vật Phẩm!', 'error');
        return;
    }

    try {
        const resp = await fetch('/api/drop-rules', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                action: 'add',
                mapId: mapId,
                itemId: itemId,
                quantity: quantity,
                ratePercent: ratePercent
            })
        });
        const result = await resp.json();
        if (resp.ok && result.status === 'success') {
            showToast(result.message, 'success');
            document.getElementById('rule-map-search').value = '';
            document.getElementById('rule-map-id').value = '-1';
            document.getElementById('rule-item-search').value = '';
            document.getElementById('rule-item-id').value = '';
            loadDropRules();
        } else {
            showToast(result.message || 'Thêm quy tắc thất bại', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối API Server', 'error');
    }
}

async function deleteDropRule(id) {
    try {
        const resp = await fetch('/api/drop-rules', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'delete', id: id })
        });
        const result = await resp.json();
        if (resp.ok && result.status === 'success') {
            showToast(result.message, 'success');
            loadDropRules();
        }
    } catch (err) {
        showToast('Lỗi kết nối API Server', 'error');
    }
}

async function toggleDropRule(id) {
    try {
        const resp = await fetch('/api/drop-rules', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'toggle', id: id })
        });
        const result = await resp.json();
        if (resp.ok && result.status === 'success') {
            showToast(result.message, 'success');
            loadDropRules();
        }
    } catch (err) {
        showToast('Lỗi kết nối API Server', 'error');
    }
}

// --- SERVER EVENTS & EXP MULTIPLIER LOGIC ---
async function loadServerEvents() {
    try {
        const resp = await fetch('/api/server-events');
        if (resp.ok) {
            const data = await resp.json();
            if (data) {
                document.getElementById('exp-rate-input').value = data.expRate || 1;
                document.getElementById('evt-lunar-new-year').checked = !!data.lunarNewYear;
                document.getElementById('evt-womens-day').checked = !!data.womensDay;
                document.getElementById('evt-halloween').checked = !!data.halloween;
                document.getElementById('evt-christmas').checked = !!data.christmas;
                document.getElementById('evt-hung-vuong').checked = !!data.hungVuong;
                document.getElementById('evt-trung-thu').checked = !!data.trungThu;
                document.getElementById('evt-top-up').checked = !!data.topUp;
            }
        }
    } catch (e) {
        console.error('Failed to load server events', e);
    }
}

function setExpPreset(val) {
    document.getElementById('exp-rate-input').value = val;
}

async function saveServerEvents(e) {
    e.preventDefault();

    const expRate = parseInt(document.getElementById('exp-rate-input').value) || 1;
    const lunarNewYear = document.getElementById('evt-lunar-new-year').checked;
    const womensDay = document.getElementById('evt-womens-day').checked;
    const halloween = document.getElementById('evt-halloween').checked;
    const christmas = document.getElementById('evt-christmas').checked;
    const hungVuong = document.getElementById('evt-hung-vuong').checked;
    const trungThu = document.getElementById('evt-trung-thu').checked;
    const topUp = document.getElementById('evt-top-up').checked;

    const btn = document.getElementById('btn-save-events');
    const origText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> ĐANG LƯU CÀI ĐẶT SỰ KIỆN...';

    try {
        const resp = await fetch('/api/server-events', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                expRate,
                lunarNewYear,
                womensDay,
                halloween,
                christmas,
                hungVuong,
                trungThu,
                topUp
            })
        });

        const result = await resp.json();

        if (resp.ok && result.status === 'success') {
            showToast(result.message, 'success');
        } else {
            showToast(result.message || 'Lưu cài đặt sự kiện thất bại', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối API Server: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = origText;
    }
}

// --- ITEM CATALOG & SEARCH ---
function renderItemCatalog(items) {
    const container = document.getElementById('item-catalog');
    container.innerHTML = '';

    if (!items || items.length === 0) {
        container.innerHTML = '<p style="color: var(--text-muted); grid-column: 1/-1; text-align: center; padding: 20px;">Không tìm thấy vật phẩm nào</p>';
        return;
    }

    items.forEach(item => {
        const iconId = (item.iconID !== undefined && item.iconID !== null) ? item.iconID : item.icon_id || item.id;
        const div = document.createElement('div');
        div.className = 'item-card';
        div.onclick = () => addItemToCart(item.id);

        div.innerHTML = `
            <div class="item-img-badge">
                <img src="/icons/${iconId}.png" onerror="this.onerror=null; this.src='/icons_x1/${iconId}.png';" class="nro-item-sprite" alt="${escapeHtml(item.name)}">
            </div>
            <div class="item-details" style="flex: 1;">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <h4 style="color: var(--text-main); font-size: 14px; font-weight: 700;">${escapeHtml(item.name)}</h4>
                    <span class="item-tag" style="background: rgba(255, 215, 0, 0.15); color: var(--gold); border: 1px solid var(--border-glass);">#${item.id}</span>
                </div>
                <p style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">${item.description ? escapeHtml(item.description) : 'Vật phẩm gốc NRO'}</p>
            </div>
        `;
        container.appendChild(div);
    });
}

function filterItems() {
    const query = document.getElementById('item-search').value.toLowerCase().trim();
    if (!query) {
        renderItemCatalog(itemTemplates.slice(0, 120));
        return;
    }
    const filtered = itemTemplates.filter(item =>
        item.id.toString().includes(query) ||
        (item.name && item.name.toLowerCase().includes(query))
    );
    renderItemCatalog(filtered.slice(0, 120));
}

// --- CART MANAGEMENT ---
function quickAddPetSkillBook(itemId) {
    switchTab('grant');
    addItemToCart(itemId);
}

function addItemToCart(itemId) {
    const item = itemTemplates.find(i => i.id === itemId);
    if (!item) return;

    const iconId = (item.iconID !== undefined && item.iconID !== null) ? item.iconID : item.icon_id || item.id;

    cartItems.push({
        id: item.id,
        name: item.name,
        description: item.description,
        iconID: iconId,
        quantity: 1,
        stars: 0,
        options: []
    });

    renderCartList();
    showToast(`⚡ Đã thêm [${item.name}] vào danh sách cấp!`, 'success');
}

function removeCartItem(index) {
    if (index >= 0 && index < cartItems.length) {
        const removed = cartItems.splice(index, 1);
        renderCartList();
        if (removed.length > 0) {
            showToast(`Đã xóa món ${removed[0].name} khỏi danh sách cấp`, 'success');
        }
    }
}

function setCartStar(cartIndex, starCount) {
    if (cartItems[cartIndex]) {
        cartItems[cartIndex].stars = starCount;
        renderCartList();
    }
}

function addCartOptionRow(cartIndex) {
    if (cartItems[cartIndex]) {
        cartItems[cartIndex].options.unshift({ id: 0, param: 10 });
        renderCartList();
    }
}

function removeCartOptionRow(cartIndex, optIndex) {
    if (cartItems[cartIndex] && cartItems[cartIndex].options[optIndex] !== undefined) {
        cartItems[cartIndex].options.splice(optIndex, 1);
        renderCartList();
    }
}

function updateCartOptionValue(cartIndex, optIndex, field, value) {
    if (cartItems[cartIndex] && cartItems[cartIndex].options[optIndex]) {
        cartItems[cartIndex].options[optIndex][field] = parseInt(value) || 0;
    }
}

function filterOptionSelect(inputEl, cartIndex, optIndex, shopOptIndex) {
    const val = inputEl.value.toLowerCase().trim();
    const container = inputEl.parentElement;
    const select = container ? container.querySelector('select') : null;
    if (!select) return;

    const listToUse = (optionTemplates && optionTemplates.length > 0) ? optionTemplates : POPULAR_OPTIONS;

    const matches = listToUse.filter(o =>
        !val ||
        o.id.toString().includes(val) ||
        (o.name && o.name.toLowerCase().includes(val))
    );

    let html = '';
    if (matches.length === 0) {
        html = '<option value="">❌ Không tìm thấy option nào khớp</option>';
    } else {
        matches.forEach(o => {
            html += `<option value="${o.id}">${o.id} - ${escapeHtml(o.name)}</option>`;
        });
    }

    select.innerHTML = html;

    if (matches.length > 0) {
        const selectedId = matches[0].id;
        select.value = selectedId;
        if (cartIndex !== null && cartIndex !== undefined && optIndex !== null && optIndex !== undefined) {
            updateCartOptionValue(cartIndex, optIndex, 'id', selectedId);
        } else if (shopOptIndex !== null && shopOptIndex !== undefined) {
            updateShopOptionValue(shopOptIndex, 'id', selectedId);
        }
    }
}

function addQuickOptionToCart(cartIndex, optId, paramVal) {
    if (cartItems[cartIndex]) {
        cartItems[cartIndex].options.unshift({ id: optId, param: paramVal });
        renderCartList();
        showToast('Đã thêm option mới vào đầu danh sách!', 'success');
    }
}

function renderCartList() {
    const container = document.getElementById('cart-items-list');
    const badge = document.getElementById('cart-count-badge');
    badge.innerText = cartItems.length + ' Món Trong Danh Sách';
    container.innerHTML = '';

    if (cartItems.length === 0) {
        container.innerHTML = '<p style="color: var(--text-muted); text-align: center; padding: 40px; border: 1px dashed var(--border-glass); border-radius: 8px;"><i class="fa-solid fa-hand-pointer" style="font-size: 24px; margin-bottom: 8px; display: block; color: var(--gold);"></i>Hãy bấm trực tiếp vào món đồ bạn muốn ở kho đồ bên trái để thêm vào danh sách cấp!</p>';
        return;
    }

    cartItems.forEach((cartItem, idx) => {
        const iconId = cartItem.iconID || cartItem.id;
        const itemCard = document.createElement('div');
        itemCard.className = 'glass';
        itemCard.style.padding = '16px';
        itemCard.style.border = '1px solid rgba(255, 215, 0, 0.3)';

        // Star buttons HTML
        let starBtnsHtml = '';
        for (let s = 0; s <= 7; s++) {
            const activeClass = cartItem.stars === s ? 'active' : '';
            const starText = s === 7 ? '⭐ 7 SAO' : s + ' Sao';
            starBtnsHtml += `<button type="button" class="star-btn ${activeClass}" onclick="setCartStar(${idx}, ${s})">${starText}</button>`;
        }

        // Custom options HTML with Quick Option Pills
        let optionsHtml = `
            <div style="margin-bottom: 8px;">
                <label style="font-size: 11px; color: var(--gold); display: block; margin-bottom: 4px;"><i class="fa-solid fa-bolt"></i> CHỌN NHANH OPTION PHỔ BIẾN:</label>
                <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                    <span class="quick-opt-pill" onclick="addQuickOptionToCart(${idx}, 50, 10)">💪 Sức đánh +10%</span>
                    <span class="quick-opt-pill" onclick="addQuickOptionToCart(${idx}, 77, 10)">❤️ HP +10%</span>
                    <span class="quick-opt-pill" onclick="addQuickOptionToCart(${idx}, 103, 10)">⚡ KI +10%</span>
                    <span class="quick-opt-pill" onclick="addQuickOptionToCart(${idx}, 94, 10)">🛡️ Giáp +10%</span>
                    <span class="quick-opt-pill" onclick="addQuickOptionToCart(${idx}, 100, 20)">💰 Vàng +20%</span>
                    <span class="quick-opt-pill" onclick="addQuickOptionToCart(${idx}, 14, 0)">🌟 Kiệt sức 0s</span>
                    <span class="quick-opt-pill" onclick="addQuickOptionToCart(${idx}, 108, 0)">👑 Không bị bem</span>
                </div>
            </div>
        `;
        const listToUse = (optionTemplates && optionTemplates.length > 0) ? optionTemplates : POPULAR_OPTIONS;

        cartItem.options.forEach((opt, optIdx) => {
            let selectOptions = '';
            listToUse.forEach(o => {
                const sel = o.id === opt.id ? 'selected' : '';
                selectOptions += `<option value="${o.id}" ${sel}>${o.id} - ${escapeHtml(o.name)}</option>`;
            });

            optionsHtml += `
                <div class="option-row" style="margin-bottom: 8px;">
                    <div style="flex: 2; display: flex; flex-direction: column; gap: 4px;">
                        <input type="text" class="input-field" placeholder="🔍 Gõ tên hoặc ID để lọc (VD: Sức đánh, 50)..." style="padding: 6px 10px; font-size: 12px; background: #ffffff; color: var(--text-main); border: 1px solid #cbd5e1;" oninput="filterOptionSelect(this, ${idx}, ${optIdx})" />
                        <select class="select-field" style="width: 100%; font-size: 12px;" onchange="updateCartOptionValue(${idx}, ${optIdx}, 'id', this.value)">
                            ${selectOptions}
                        </select>
                    </div>
                    <input type="number" class="input-field" value="${opt.param}" style="flex: 1;" placeholder="Chỉ số (Param)..." oninput="updateCartOptionValue(${idx}, ${optIdx}, 'param', this.value)" required>
                    <button type="button" class="btn-danger" style="align-self: flex-end; padding: 8px;" onclick="removeCartOptionRow(${idx}, ${optIdx})">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            `;
        });

        itemCard.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <div class="item-img-badge">
                        <img src="/icons/${iconId}.png" onerror="this.onerror=null; this.src='/icons_x1/${iconId}.png';" class="nro-item-sprite" alt="${escapeHtml(cartItem.name)}">
                    </div>
                    <div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <h4 style="color: var(--text-main); font-size: 15px; font-weight: 700; margin: 0;">Món ${idx + 1}: ${escapeHtml(cartItem.name)}</h4>
                            <span class="item-tag" style="background: rgba(255, 215, 0, 0.15); color: var(--gold); border: 1px solid var(--gold);">#${cartItem.id}</span>
                        </div>
                        <p style="font-size: 12px; color: var(--text-muted);">${cartItem.description ? escapeHtml(cartItem.description) : 'Vật phẩm gốc NRO'}</p>
                    </div>
                </div>
                <button type="button" class="btn-danger" style="padding: 6px 12px; font-size: 12px;" onclick="removeCartItem(${idx})">
                    <i class="fa-solid fa-trash"></i> Xóa
                </button>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 2fr; gap: 12px; margin-bottom: 12px;">
                <div class="form-group" style="margin-bottom: 0;">
                    <label style="font-size: 12px;"><i class="fa-solid fa-layer-group"></i> Số Lượng</label>
                    <input type="number" class="input-field" value="${cartItem.quantity}" min="1" max="9999" onchange="cartItems[${idx}].quantity = parseInt(this.value) || 1">
                </div>
                <div class="form-group" style="margin-bottom: 0;">
                    <label style="font-size: 12px;"><i class="fa-solid fa-star" style="color: var(--gold);"></i> Đục Lỗ Sao</label>
                    <div class="star-selector">${starBtnsHtml}</div>
                </div>
            </div>

            <div class="form-group" style="margin-bottom: 0;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                    <label style="font-size: 12px; margin: 0;"><i class="fa-solid fa-bolt"></i> Chỉ Số Option Đồ</label>
                    <button type="button" class="btn-secondary" style="font-size: 11px; padding: 4px 8px;" onclick="addCartOptionRow(${idx})">
                        <i class="fa-solid fa-plus"></i> Thêm Chỉ Số
                    </button>
                </div>
                <div>${optionsHtml}</div>
            </div>
        `;
        container.appendChild(itemCard);
    });
}

// --- PET GRANT STUDIO LOGIC ---
function handlePetPlayerSearchInput() {
    const input = document.getElementById('target-player-pet');
    const val = input.value.trim().toLowerCase();
    const dropdown = document.getElementById('pet-player-suggestions');

    checkPetPlayerStatus();

    if (!val) {
        dropdown.style.display = 'none';
        return;
    }

    const matches = playersList.filter(p => p.name && p.name.toLowerCase().includes(val));
    if (matches.length === 0) {
        dropdown.style.display = 'none';
        return;
    }

    dropdown.innerHTML = '';
    matches.slice(0, 10).forEach(p => {
        const item = document.createElement('div');
        item.className = 'suggestion-item';
        const badge = p.online ? '<span class="badge-online" style="font-size: 10px;">ONLINE</span>' : '<span class="badge-offline" style="font-size: 10px;">OFFLINE</span>';
        item.innerHTML = `
            <span><i class="fa-solid fa-user"></i> <strong>${escapeHtml(p.name)}</strong> (ID #${p.id})</span>
            ${badge}
        `;
        item.onclick = () => {
            document.getElementById('target-player-pet').value = p.name;
            dropdown.style.display = 'none';
            checkPetPlayerStatus();
        };
        dropdown.appendChild(item);
    });

    dropdown.style.display = 'block';
}

function checkPetPlayerStatus() {
    const name = document.getElementById('target-player-pet').value.trim();
    const badge = document.getElementById('pet-player-status-badge');

    if (!name) {
        badge.className = 'badge-offline';
        badge.innerText = 'Chưa chọn';
        return;
    }

    const match = playersList.find(p => p.name && p.name.toLowerCase() === name.toLowerCase());
    if (match) {
        if (match.online) {
            badge.className = 'badge-online';
            badge.innerText = 'ONLINE LIVE';
        } else {
            badge.className = 'badge-offline';
            badge.innerText = 'OFFLINE';
        }
    } else {
        badge.className = 'badge-offline';
        badge.innerText = 'NHẬN DIỆN TỰ ĐỘNG';
    }
}

function setPetPowerPreset(val) {
    const pInput = document.getElementById('pet-power-input');
    const tInput = document.getElementById('pet-tiemnang-input');
    if (pInput) pInput.value = val;
    if (tInput) tInput.value = val;
}

function handlePetTypeChange() {
    const petType = parseInt(document.getElementById('pet-type-select').value) || 0;
    const pInput = document.getElementById('pet-power-input');
    const tInput = document.getElementById('pet-tiemnang-input');

    let minP = 2000;
    if (petType === 1) minP = 1500000;
    else if (petType >= 2) minP = 40000000000;

    if (pInput) pInput.value = minP;
    if (tInput) tInput.value = minP;
}

async function executeGrantPet(e) {
    e.preventDefault();

    const playerNameInput = document.getElementById('target-player-pet');
    const playerName = playerNameInput ? playerNameInput.value.trim() : '';
    const petType = parseInt(document.getElementById('pet-type-select').value) || 0;
    const petGender = parseInt(document.getElementById('pet-gender-select').value) || 0;

    const pEl = document.getElementById('pet-power-input');
    const tEl = document.getElementById('pet-tiemnang-input');
    const powerRaw = pEl ? pEl.value.toString().replace(/,/g, '').replace(/\./g, '').trim() : '2000';
    const tiemNangRaw = tEl ? tEl.value.toString().replace(/,/g, '').replace(/\./g, '').trim() : powerRaw;

    const power = parseInt(powerRaw) || 2000;
    const tiemNang = parseInt(tiemNangRaw) || power;

    if (!playerName) {
        showToast('Vui lòng nhập tên nhân vật nhận đệ tử!', 'error');
        return;
    }

    const btn = document.getElementById('btn-grant-pet-action');
    const origText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> ĐANG GỬI CẤP/ĐỔI ĐỆ TỬ...';

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 10000);

    try {
        const resp = await fetch('/api/grant-pet', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            signal: controller.signal,
            body: JSON.stringify({
                playerName: playerName,
                petType: petType,
                petGender: petGender,
                power: power,
                tiemNang: tiemNang
            })
        });
        clearTimeout(timeoutId);

        const result = await resp.json();

        if (resp.ok && result.status === 'success') {
            showToast(result.message, 'success');
            loadStats();
        } else {
            showToast(result.message || 'Cấp đệ tử thất bại', 'error');
        }
    } catch (err) {
        clearTimeout(timeoutId);
        if (err.name === 'AbortError') {
            showToast('Quá thời gian chờ (Timeout 10s). Hãy đảm bảo Game Server đang chạy!', 'error');
        } else {
            showToast('Lỗi kết nối API Server: ' + err.message, 'error');
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = origText;
    }
}

// --- PLAYER AUTO-SUGGEST & STATUS FOR ITEM GRANT ---
function handlePlayerSearchInput() {
    const input = document.getElementById('target-player');
    const val = input.value.trim().toLowerCase();
    const dropdown = document.getElementById('player-suggestions');

    checkTargetPlayerStatus();

    if (!val) {
        dropdown.style.display = 'none';
        return;
    }

    const matches = playersList.filter(p => p.name && p.name.toLowerCase().includes(val));
    if (matches.length === 0) {
        dropdown.style.display = 'none';
        return;
    }

    dropdown.innerHTML = '';
    matches.slice(0, 10).forEach(p => {
        const item = document.createElement('div');
        item.className = 'suggestion-item';
        const badge = p.online ? '<span class="badge-online" style="font-size: 10px;">ONLINE</span>' : '<span class="badge-offline" style="font-size: 10px;">OFFLINE</span>';
        item.innerHTML = `
            <span><i class="fa-solid fa-user"></i> <strong>${escapeHtml(p.name)}</strong> (ID #${p.id})</span>
            ${badge}
        `;
        item.onclick = () => selectPlayerSuggestion(p.name);
        dropdown.appendChild(item);
    });

    dropdown.style.display = 'block';
}

function showPlayerSuggestions() {
    if (playersList.length > 0) {
        handlePlayerSearchInput();
    }
}

function selectPlayerSuggestion(name) {
    document.getElementById('target-player').value = name;
    document.getElementById('player-suggestions').style.display = 'none';
    checkTargetPlayerStatus();
}

function setupOutsideClickListener() {
    document.addEventListener('click', (e) => {
        const dropdown1 = document.getElementById('player-suggestions');
        const dropdown2 = document.getElementById('pet-player-suggestions');
        const dropdownMap = document.getElementById('map-suggestions');
        const dropdownItem = document.getElementById('rule-item-suggestions');
        const dropdownShopItem = document.getElementById('shop-item-suggestions');

        const input1 = document.getElementById('target-player');
        const input2 = document.getElementById('target-player-pet');
        const inputMap = document.getElementById('rule-map-search');
        const inputItem = document.getElementById('rule-item-search');
        const inputShopItem = document.getElementById('shop-item-search');

        if (dropdown1 && !dropdown1.contains(e.target) && e.target !== input1) dropdown1.style.display = 'none';
        if (dropdown2 && !dropdown2.contains(e.target) && e.target !== input2) dropdown2.style.display = 'none';
        if (dropdownMap && !dropdownMap.contains(e.target) && e.target !== inputMap) dropdownMap.style.display = 'none';
        if (dropdownItem && !dropdownItem.contains(e.target) && e.target !== inputItem) dropdownItem.style.display = 'none';
        if (dropdownShopItem && !dropdownShopItem.contains(e.target) && e.target !== inputShopItem) dropdownShopItem.style.display = 'none';
    });
}

function checkTargetPlayerStatus() {
    const name = document.getElementById('target-player').value.trim();
    const badge = document.getElementById('player-status-badge');

    if (!name) {
        badge.className = 'badge-offline';
        badge.innerText = 'Chưa chọn';
        return;
    }

    const match = playersList.find(p => p.name && p.name.toLowerCase() === name.toLowerCase());
    if (match) {
        if (match.online) {
            badge.className = 'badge-online';
            badge.innerText = 'ONLINE LIVE';
        } else {
            badge.className = 'badge-offline';
            badge.innerText = 'OFFLINE';
        }
    } else {
        badge.className = 'badge-offline';
        badge.innerText = 'NHẬN DIỆN TỰ ĐỘNG';
    }
}

function quickSelectPlayer(name) {
    document.getElementById('target-player').value = name;
    document.getElementById('target-player-pet').value = name;
    switchTab('grant');
    checkTargetPlayerStatus();
    checkPetPlayerStatus();
}

// --- EXECUTE BATCH REALTIME ITEM GRANT ---
async function executeBatchGrant(e) {
    e.preventDefault();

    const playerName = document.getElementById('target-player').value.trim();

    if (!playerName) {
        showToast('Vui lòng nhập hoặc chọn tên nhân vật nhận đồ!', 'error');
        return;
    }

    if (cartItems.length === 0) {
        showToast('Danh sách vật phẩm cấp đang trống. Vui lòng bấm vào món đồ bên trái!', 'error');
        return;
    }

    const itemsPayload = cartItems.map(item => ({
        itemId: item.id,
        quantity: item.quantity,
        stars: item.stars,
        options: item.options
    }));

    const btn = document.getElementById('btn-grant-action');
    const origText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> ĐANG GỬI CẤP TOÀN BỘ DANH SÁCH...';

    try {
        const resp = await fetch('/api/grant-item-batch', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerName: playerName,
                items: itemsPayload
            })
        });
        const result = await resp.json();

        if (resp.ok && result.status === 'success') {
            showToast(result.message, 'success');
            loadStats();
        } else {
            showToast(result.message || 'Cấp đồ thất bại', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối API Server: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = origText;
    }
}

// --- ACCOUNT TABLE RENDER & MANAGEMENT ---
async function loadAccountData() {
    try {
        const resp = await fetch('/api/admin/accounts');
        if (resp.ok) {
            accountsList = await resp.json();
            renderAccountTable();
        }
    } catch (e) {
        console.error('Failed to load accounts', e);
    }
}

function renderAccountTable() {
    const tbody = document.getElementById('account-table-body');
    if (!tbody) return;
    const query = (document.getElementById('account-table-search')?.value || '').toLowerCase().trim();
    tbody.innerHTML = '';

    const filtered = accountsList.filter(a => !query || (a.username && a.username.toLowerCase().includes(query)));

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 20px;">Không tìm thấy tài khoản nào</td></tr>';
        return;
    }

    filtered.forEach(a => {
        const tr = document.createElement('tr');

        const adminBadge = a.admin === 1
            ? '<span style="background: var(--teamobi-orange-bg); color: var(--teamobi-orange-dark); padding: 2px 8px; border-radius: 4px; font-weight: bold; font-size: 11px;">ADMIN</span>'
            : '<span style="color: var(--text-muted); font-size: 11px;">User</span>';

        const activeBadge = a.active === 1
            ? '<span class="badge-online">HOẠT ĐỘNG</span>'
            : '<span class="badge-offline">BỊ KHÓA</span>';

        const createDate = a.create_time ? new Date(a.create_time).toLocaleDateString('vi-VN') : 'N/A';

        tr.innerHTML = `
            <td data-label="ID" style="padding: 12px; color: var(--text-muted);">#${a.id}</td>
            <td data-label="Tài Khoản" style="padding: 12px; font-weight: 700; color: var(--text-main);">${escapeHtml(a.username)}</td>
            <td data-label="Mật Khẩu" style="padding: 12px; color: var(--cyan); font-family: monospace;">${escapeHtml(a.password)}</td>
            <td data-label="Số NV" style="padding: 12px; color: var(--teamobi-orange-dark); font-weight: 700;">${a.player_count || 0} NV</td>
            <td data-label="Quyền" style="padding: 12px;">${adminBadge}</td>
            <td data-label="Trạng Thái" style="padding: 12px;">${activeBadge}</td>
            <td data-label="Ngày Tạo" style="padding: 12px; color: var(--text-muted); font-size: 12px;">${createDate}</td>
            <td data-label="Thao Tác" style="padding: 12px; display: flex; gap: 6px; flex-wrap: wrap;">
                <button class="btn-secondary" style="padding: 4px 8px; font-size: 11px; color: var(--teamobi-orange-dark);" onclick="toggleAccountAdmin(${a.id}, ${a.admin || 0})">
                    <i class="fa-solid fa-user-shield"></i> ${a.admin === 1 ? 'Hạ Admin' : 'Cấp Admin'}
                </button>
                <button class="btn-secondary" style="padding: 4px 8px; font-size: 11px; color: var(--cyan);" onclick="changeAccountPassword(${a.id}, '${escapeHtml(a.username)}')">
                    <i class="fa-solid fa-key"></i> Đổi MK
                </button>
                <button class="btn-secondary" style="padding: 4px 8px; font-size: 11px;" onclick="toggleAccountLock(${a.id}, ${a.active})">
                    <i class="fa-solid ${a.active === 1 ? 'fa-lock' : 'fa-lock-open'}"></i> ${a.active === 1 ? 'Khóa' : 'Mở'}
                </button>
                <button class="btn-danger" style="padding: 4px 8px; font-size: 11px;" onclick="deleteAccount(${a.id}, '${escapeHtml(a.username)}')">
                    <i class="fa-solid fa-trash"></i> Xóa
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// --- MODAL UTILS ---
function showModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) {
        el.style.setProperty('display', 'block', 'important');
    }
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) {
        el.style.setProperty('display', 'none', 'important');
    }
}

function customConfirm(title, message, onConfirm) {
    const titleEl = document.getElementById('confirm-modal-title');
    const msgEl = document.getElementById('confirm-modal-message');
    const btn = document.getElementById('modal-confirm-action-btn');

    if (titleEl) titleEl.innerHTML = `<i class="fa-solid fa-triangle-exclamation" style="font-size: 22px; color: var(--gold);"></i> ${escapeHtml(title)}`;
    if (msgEl) msgEl.innerHTML = message;

    btn.onclick = async () => {
        closeModal('modal-confirm-action');
        if (onConfirm) await onConfirm();
    };

    showModal('modal-confirm-action');
}

function changeAccountPassword(id, username) {
    const nameEl = document.getElementById('change-pass-user-name');
    if (nameEl) nameEl.innerText = username;
    const input = document.getElementById('modal-new-password-input');
    if (input) input.value = '';

    const confirmBtn = document.getElementById('modal-confirm-change-pass-btn');
    if (confirmBtn) {
        confirmBtn.onclick = async () => {
            const newPass = input ? input.value.trim() : '';
            if (!newPass) {
                showToast('Vui lòng nhập mật khẩu mới', 'error');
                return;
            }
            try {
                const resp = await fetch('/api/admin/account/update', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id, password: newPass })
                });
                const data = await resp.json();
                showToast(data.message, data.status === 'success' ? 'success' : 'error');
                closeModal('modal-change-password');
                loadAccountData();
            } catch (e) {
                showToast('Lỗi khi đổi mật khẩu tài khoản', 'error');
            }
        };
    }

    showModal('modal-change-password');
}

function deleteAccount(id, username) {
    customConfirm(
        'XÁC NHẬN XÓA TÀI KHOẢN',
        `Bạn có chắc chắn muốn <strong style="color: var(--red);">XÓA VĨNH VIỄN</strong> tài khoản <strong style="color: var(--gold);">${escapeHtml(username)}</strong> và toàn bộ dữ liệu nhân vật liên quan?`,
        async () => {
            try {
                const resp = await fetch('/api/admin/account/delete', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id })
                });
                const data = await resp.json();
                showToast(data.message, data.status === 'success' ? 'success' : 'error');
                loadAccountData();
            } catch (e) {
                showToast('Lỗi khi xóa tài khoản', 'error');
            }
        }
    );
}

async function editItemTemplateName(itemId, oldName) {
    const newName = prompt(`Nhập TÊN MỚI cho Vật Phẩm #${itemId} (Hiện tại: ${oldName}):`, oldName);
    if (!newName || !newName.trim() || newName.trim() === oldName) return;

    try {
        const resp = await fetch('/api/admin/item-template/update-name', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: itemId, name: newName.trim() })
        });
        const data = await resp.json();
        showToast(data.message, data.status === 'success' ? 'success' : 'error');
        await loadItemTemplates();
        if (typeof renderCartList === 'function') renderCartList();
    } catch (e) {
        showToast('Lỗi khi đổi tên vật phẩm', 'error');
    }
}

async function toggleAccountAdmin(id, currentAdmin) {
    const newAdmin = currentAdmin === 1 ? 0 : 1;
    try {
        const resp = await fetch('/api/admin/account/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id, admin: newAdmin, is_admin: newAdmin })
        });
        const data = await resp.json();
        showToast(data.message, data.status === 'success' ? 'success' : 'error');
        loadAccountData();
    } catch (e) {
        showToast('Lỗi khi cập nhật quyền Admin', 'error');
    }
}

async function toggleAccountLock(id, currentActive) {
    const newActive = currentActive === 1 ? 0 : 1;
    try {
        const resp = await fetch('/api/admin/account/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id, active: newActive })
        });
        const data = await resp.json();
        showToast(data.message, data.status === 'success' ? 'success' : 'error');
        loadAccountData();
    } catch (e) {
        showToast('Lỗi khi cập nhật tài khoản', 'error');
    }
}

async function openQuickCreateAccountModal() {
    const user = prompt('Nhập tên tài khoản mới:');
    if (!user || !user.trim()) return;
    const pass = prompt('Nhập mật khẩu:');
    if (!pass || !pass.trim()) return;

    try {
        const resp = await fetch('/api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user.trim(), password: pass.trim() })
        });
        const data = await resp.json();
        showToast(data.message, data.status === 'success' ? 'success' : 'error');
        loadAccountData();
    } catch (e) {
        showToast('Lỗi khi tạo tài khoản', 'error');
    }
}

function nextPlayerTask(playerName) {
    customConfirm(
        'XÁC NHẬN QUA NHIỆM VỤ',
        `Bạn có chắc chắn muốn chuyển sang nhiệm vụ tiếp theo cho nhân vật <strong style="color: var(--gold);">${escapeHtml(playerName)}</strong>?`,
        async () => {
            try {
                const resp = await fetch('/api/next-task', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ playerName })
                });
                const data = await resp.json();
                showToast(data.message, data.status === 'success' ? 'success' : 'error');
                await loadPlayers();
            } catch (e) {
                showToast('Lỗi khi chuyển nhiệm vụ', 'error');
            }
        }
    );
}

let currentRenameOldName = '';
function changePlayerName(idOrName, nameStr) {
    const targetName = typeof nameStr === 'string' && nameStr ? nameStr : idOrName;
    currentRenameOldName = String(targetName);
    const label = document.getElementById('rename-player-old-name');
    const input = document.getElementById('rename-player-new-name-input');
    if (label) label.textContent = currentRenameOldName;
    if (input) input.value = currentRenameOldName;
    showModal('modal-rename-player');
}

async function submitRenamePlayer() {
    const input = document.getElementById('rename-player-new-name-input');
    if (!input) return;
    const newName = input.value.trim();
    if (!newName) {
        showToast('Vui lòng nhập tên nhân vật mới', 'error');
        return;
    }
    if (newName === currentRenameOldName) {
        showToast('Tên nhân vật mới trùng với tên cũ', 'error');
        return;
    }
    try {
        const resp = await fetch('/api/rename-player', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ oldName: currentRenameOldName, newName })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message || 'Đổi tên thành công!', 'success');
            closeModal('modal-rename-player');
            await loadPlayers();
        } else {
            showToast(data.message || 'Đổi tên thất bại', 'error');
        }
    } catch (e) {
        showToast('Lỗi kết nối khi đổi tên nhân vật', 'error');
    }
}

function openModal(id) {
    if (typeof showModal === 'function') showModal(id);
    else {
        const el = document.getElementById(id);
        if (el) el.style.setProperty('display', 'flex', 'important');
    }
}

function openAdjustPlayerEventPointModal(playerName, currentPoint) {
    const targetInput = document.getElementById('modal-event-point-target-name');
    const valInput = document.getElementById('modal-event-point-val-input');
    const actionSelect = document.getElementById('modal-event-point-action-select');

    if (targetInput) targetInput.value = playerName;
    if (valInput) valInput.value = currentPoint || 0;
    if (actionSelect) actionSelect.value = 'set';

    showModal('modal-adjust-event-point');
}

function setModalEventPointPreset(val) {
    const valInput = document.getElementById('modal-event-point-val-input');
    const actionSelect = document.getElementById('modal-event-point-action-select');
    if (!valInput) return;
    if (actionSelect && actionSelect.value === 'add') {
        const cur = parseInt(valInput.value) || 0;
        valInput.value = cur + val;
    } else {
        valInput.value = val;
    }
}

async function submitAdjustPlayerEventPoint() {
    const playerName = document.getElementById('modal-event-point-target-name').value;
    const action = document.getElementById('modal-event-point-action-select').value;
    const eventPoint = parseInt(document.getElementById('modal-event-point-val-input').value) || 0;

    if (!playerName) return showToast('Thiếu tên nhân vật!', 'error');

    try {
        const resp = await fetch('/api/admin/player/update-event-point', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerName, action, eventPoint })
        });
        const data = await resp.json();
        showToast(data.message, data.status === 'success' ? 'success' : 'error');
        closeModal('modal-adjust-event-point');
        await loadPlayers();
    } catch (e) {
        showToast('Lỗi khi cập nhật Điểm Sự Kiện', 'error');
    }
}

function handleEventPointPlayerSearchInput() {
    const input = document.getElementById('target-player-event-point');
    const dropdown = document.getElementById('event-point-player-suggestions');
    const badge = document.getElementById('event-point-player-status-badge');
    if (!input || !dropdown) return;

    const val = input.value.trim().toLowerCase();
    if (!val) {
        dropdown.style.display = 'none';
        if (badge) { badge.className = 'badge-offline'; badge.innerText = 'Chưa chọn'; }
        return;
    }

    const matches = playersList.filter(p => p.name && p.name.toLowerCase().includes(val));
    if (matches.length === 0) {
        dropdown.style.display = 'none';
        if (badge) { badge.className = 'badge-offline'; badge.innerText = 'Chưa chọn'; }
        return;
    }

    dropdown.innerHTML = '';
    matches.forEach(p => {
        const item = document.createElement('div');
        item.className = 'suggestion-item';
        item.innerHTML = `<strong>${escapeHtml(p.name)}</strong> <span style="font-size: 11px; color: var(--text-muted);">(${p.online ? 'Online' : 'Offline'} - ⭐ ${p.eventPoint || 0} điểm)</span>`;
        item.onclick = () => {
            input.value = p.name;
            dropdown.style.display = 'none';
            if (badge) {
                badge.className = p.online ? 'badge-online' : 'badge-offline';
                badge.innerText = p.online ? 'ONLINE' : 'OFFLINE';
            }
        };
        dropdown.appendChild(item);
    });
    dropdown.style.display = 'block';
}

function setDirectEventPointPreset(val) {
    const valInput = document.getElementById('direct-event-point-val-input');
    const actionSelect = document.getElementById('direct-event-point-action-select');
    if (!valInput) return;
    if (actionSelect && actionSelect.value === 'add') {
        const cur = parseInt(valInput.value) || 0;
        valInput.value = cur + val;
    } else {
        valInput.value = val;
    }
}

async function executeDirectEventPointUpdate(e) {
    if (e) e.preventDefault();
    const playerName = document.getElementById('target-player-event-point').value.trim();
    const action = document.getElementById('direct-event-point-action-select').value;
    const eventPoint = parseInt(document.getElementById('direct-event-point-val-input').value) || 0;

    if (!playerName) return showToast('Vui lòng nhập tên nhân vật!', 'error');

    try {
        const resp = await fetch('/api/admin/player/update-event-point', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerName, action, eventPoint })
        });
        const data = await resp.json();
        showToast(data.message, data.status === 'success' ? 'success' : 'error');
        await loadPlayers();
    } catch (err) {
        showToast('Lỗi khi cập nhật Điểm Sự Kiện', 'error');
    }
}



function renderPlayerTable() {
    const tbody = document.getElementById('player-table-body');
    if (!tbody) return;
    const query = document.getElementById('player-table-search').value.toLowerCase().trim();
    tbody.innerHTML = '';

    const filtered = playersList.filter(p => !query || (p.name && p.name.toLowerCase().includes(query)));

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 20px;">Không có dữ liệu nhân vật</td></tr>';
        return;
    }

    filtered.forEach(p => {
        const tr = document.createElement('tr');

        const genderText = p.gender === 0 ? 'Trái Đất' : p.gender === 1 ? 'Namếc' : 'Xayda';
        const statusBadge = p.online
            ? '<span class="badge-online">ONLINE</span>'
            : '<span class="badge-offline">OFFLINE</span>';

        const avatarSrc = p.avatarUrl || `/icons/${p.avatarId || 521}.png`;
        const taskBadge = `<span style="background: var(--teamobi-orange-bg); color: var(--teamobi-orange-dark); border: 1px solid var(--teamobi-orange-border); padding: 4px 8px; border-radius: 6px; font-weight: 700; font-size: 11px;">#${p.taskId !== undefined ? p.taskId : 0} - ${escapeHtml(p.taskName || 'Nhiệm vụ')}</span>`;
        const eventPointBadge = `<span style="background: #fff3e0; color: #e65100; border: 1px solid #ffb74d; padding: 4px 8px; border-radius: 6px; font-weight: 800; font-size: 12px; white-space: nowrap;"><i class="fa-solid fa-star"></i> ${(p.eventPoint !== undefined ? p.eventPoint : 0).toLocaleString('vi-VN')} đ</span>`;

        tr.innerHTML = `
            <td data-label="ID" style="padding: 12px; color: var(--text-muted);">#${p.id}</td>
            <td data-label="Tên Nhân Vật" style="padding: 12px; font-weight: 700; color: var(--text-main);">
                <div style="display: flex; align-items: center; gap: 10px;">
                    <img src="${avatarSrc}" onerror="this.src='/icons/521.png';" style="width: 34px; height: 34px; object-fit: contain; background: var(--teamobi-orange-bg); border-radius: 8px; padding: 2px; border: 1px solid var(--teamobi-orange-border);" />
                    <span>${escapeHtml(p.name)}</span>
                </div>
            </td>
            <td data-label="Tài Khoản" style="padding: 12px; color: var(--teamobi-orange-dark); font-size: 13px;">${escapeHtml(p.username || 'N/A')}</td>
            <td data-label="Hành Tinh" style="padding: 12px; color: var(--cyan);">${genderText}</td>
            <td data-label="Nhiệm Vụ" style="padding: 12px;">${taskBadge}</td>
            <td data-label="Điểm Sự Kiện" style="padding: 12px;">${eventPointBadge}</td>
            <td data-label="Trạng Thái" style="padding: 12px;">${statusBadge}</td>
            <td data-label="Thao Tác" style="padding: 10px 12px; white-space: nowrap;">
                <div style="display: flex; gap: 6px; flex-wrap: nowrap; align-items: center;">
                    <button class="btn-secondary" style="padding: 5px 10px; font-size: 11px; background: rgba(255, 102, 0, 0.15); color: var(--teamobi-orange-dark); font-weight: 800; border: 1px solid var(--teamobi-orange-border); white-space: nowrap;" onclick="openPlayerInventoryModal('${escapeHtml(p.name)}')">
                        <i class="fa-solid fa-briefcase"></i> 🎒 Hành Trang
                    </button>
                    <button class="btn-secondary" style="padding: 5px 10px; font-size: 11px; background: #fff3e0; color: #e65100; font-weight: 800; border: 1px solid #ffb74d; white-space: nowrap;" onclick="openAdjustPlayerEventPointModal('${escapeHtml(p.name)}', ${p.eventPoint || 0})">
                        <i class="fa-solid fa-star"></i> ⭐ Đổi Điểm
                    </button>
                    <button class="btn-secondary" style="padding: 5px 10px; font-size: 11px; background: rgba(16, 185, 129, 0.15); color: #10b981; font-weight: 700; border: 1px solid #10b981; white-space: nowrap;" onclick="openAdjustPlayerPowerModal('${escapeHtml(p.name)}')">
                        <i class="fa-solid fa-bolt"></i> ⚡ SM
                    </button>
                    <button class="btn-secondary" style="padding: 5px 10px; font-size: 11px; white-space: nowrap;" onclick="quickSelectPlayer('${escapeHtml(p.name)}')">
                        <i class="fa-solid fa-gift"></i> Cấp Đồ
                    </button>
                    <button class="btn-secondary" style="padding: 5px 10px; font-size: 11px; background: rgba(0, 243, 255, 0.15); color: var(--cyan); white-space: nowrap;" onclick="changePlayerName(${p.id}, '${escapeHtml(p.name)}')">
                        <i class="fa-solid fa-pen-to-square"></i> Đổi Tên
                    </button>
                    <button class="btn-secondary" style="padding: 5px 10px; font-size: 11px; background: var(--teamobi-orange-bg); color: var(--teamobi-orange-dark); font-weight: 700; border: 1px solid var(--teamobi-orange-border); white-space: nowrap;" onclick="nextPlayerTask('${escapeHtml(p.name)}')">
                        <i class="fa-solid fa-forward-step"></i> Qua NV
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function setModalPowerPreset(val) {
    const pInput = document.getElementById('modal-power-input');
    const tInput = document.getElementById('modal-tiemnang-input');
    if (pInput) pInput.value = val;
    if (tInput) tInput.value = val;
}

function openAdjustPlayerPowerModal(playerName) {
    const nameEl = document.getElementById('adjust-power-player-name');
    if (nameEl) nameEl.innerText = playerName;

    showModal('modal-adjust-power');

    const confirmBtn = document.getElementById('modal-confirm-adjust-power-btn');
    confirmBtn.onclick = async () => {
        const action = document.getElementById('modal-power-action').value;
        const power = parseInt(document.getElementById('modal-power-input').value) || 0;
        const tiemNang = parseInt(document.getElementById('modal-tiemnang-input').value) || 0;

        try {
            const resp = await fetch('/api/adjust-player-power', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ playerName, action, power, tiemNang })
            });
            const data = await resp.json();
            closeModal('modal-adjust-power');
            showToast(data.message, data.status === 'success' ? 'success' : 'error');
            loadPlayers();
        } catch (e) {
            closeModal('modal-adjust-power');
            showToast('Không thể kết nối đến Game Server', 'error');
        }
    };
}

// --- REMOVED DUPLICATE TAB SWITCHER LOGIC ---

// --- GIFTCODE MANAGER LOGIC ---
let currentGcItems = [];

function generateRandomGcCode() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code = 'NRO-';
    for (let i = 0; i < 6; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    const input = document.getElementById('gc-code-input');
    if (input) input.value = code;
}

function addQuickGcCurrency(id, quantity, name) {
    const existing = currentGcItems.find(item => item.id === id);
    if (existing) {
        existing.quantity += quantity;
    } else {
        currentGcItems.unshift({
            id: id,
            quantity: quantity,
            name: name,
            options: []
        });
    }
    renderGcItemsRows();
    showToast(`Đã thêm ${name} vào GiftCode!`, 'success');
}

function addGcItemRow() {
    currentGcItems.unshift({
        id: 190,
        quantity: 1,
        name: 'Vật phẩm',
        options: []
    });
    renderGcItemsRows();
}

function removeGcItemRow(index) {
    currentGcItems.splice(index, 1);
    renderGcItemsRows();
}

function updateGcItemField(index, field, val) {
    if (!currentGcItems[index]) return;
    if (field === 'id') {
        currentGcItems[index].id = parseInt(val) || 0;
        const itemObj = itemTemplates.find(i => i.id === currentGcItems[index].id);
        if (itemObj) currentGcItems[index].name = itemObj.name;
    } else if (field === 'quantity') {
        currentGcItems[index].quantity = parseInt(val) || 1;
    }
}

function addGcItemOption(index) {
    if (!currentGcItems[index]) return;
    currentGcItems[index].options.unshift({ id: 50, param: 10 });
    renderGcItemsRows();
}

function removeGcItemOption(itemIndex, optIndex) {
    if (!currentGcItems[itemIndex]) return;
    currentGcItems[itemIndex].options.splice(optIndex, 1);
    renderGcItemsRows();
}

function updateGcOptionField(itemIndex, optIndex, field, val) {
    if (!currentGcItems[itemIndex] || !currentGcItems[itemIndex].options[optIndex]) return;
    currentGcItems[itemIndex].options[optIndex][field] = parseInt(val) || 0;
}

function renderGcItemsRows() {
    const container = document.getElementById('gc-items-list');
    if (!container) return;
    container.innerHTML = '';

    if (currentGcItems.length === 0) {
        container.innerHTML = '<div style="color: var(--text-muted); font-size: 12px; padding: 10px 0;">Chưa có phần quà nào. Hãy bấm các nút bên trên để thêm Tiền / Vật phẩm vào GiftCode!</div>';
        return;
    }

    currentGcItems.forEach((item, index) => {
        const row = document.createElement('div');
        row.style.padding = '14px';
        row.style.marginBottom = '12px';
        row.style.background = '#ffffff';
        row.style.border = '1px solid #ffe0b2';
        row.style.borderRadius = '10px';
        row.style.boxShadow = '0 2px 8px rgba(0,0,0,0.04)';

        const isCurrency = item.id < 0;

        let optionsHtml = '';
        if (!isCurrency) {
            optionsHtml = `
                <div style="margin-top: 10px; border-top: 1px dashed #ffd8a8; padding-top: 10px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                        <span style="font-size: 12px; font-weight: 700; color: #e65100;"><i class="fa-solid fa-bolt"></i> Option / Chỉ Số Vật Phẩm</span>
                        <button type="button" class="btn-secondary" style="font-size: 11px; padding: 4px 10px; background: #fff3e0; color: #e65100; border: 1px solid #ffb74d; font-weight: 700;" onclick="addGcItemOption(${index})">+ Thêm Option</button>
                    </div>
                    ${item.options.map((opt, optIdx) => `
                        <div style="display: flex; gap: 8px; margin-bottom: 6px; align-items: center; flex-wrap: wrap;">
                            <div style="flex: 1; min-width: 180px; position: relative;">
                                <input type="text" class="input-field" style="font-size: 11px; padding: 4px 8px; margin-bottom: 4px; background: #fafafa;"
                                    placeholder="🔍 Gõ mã ID hoặc tên option..."
                                    oninput="filterGcOptionSelect(this, ${index}, ${optIdx})">
                                <select class="select-field" style="width: 100%; font-size: 12px; padding: 6px 10px; background: #ffffff; color: #1e293b; border: 1px solid #cbd5e1;" onchange="updateGcOptionField(${index}, ${optIdx}, 'id', this.value)">
                                    ${optionTemplates.map(o => `<option value="${o.id}" ${o.id === opt.id ? 'selected' : ''}>#${o.id} - ${escapeHtml(o.name)}</option>`).join('')}
                                </select>
                            </div>
                            <input type="number" class="input-field" style="width: 110px; font-size: 12px; padding: 6px 10px; background: #ffffff; color: #1e293b; border: 1px solid #cbd5e1; font-weight: 700;" value="${opt.param}" placeholder="Param" onchange="updateGcOptionField(${index}, ${optIdx}, 'param', this.value)">
                            <button type="button" class="btn-danger" style="font-size: 11px; padding: 6px 10px;" onclick="removeGcItemOption(${index}, ${optIdx})">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </div>
                    `).join('')}
                </div>
            `;
        }

        row.innerHTML = `
            <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                <span style="font-weight: bold; color: ${isCurrency ? 'var(--teamobi-orange-dark)' : 'var(--cyan)'}; font-size: 13px;">
                    ${isCurrency ? (item.id === -1 ? '💰 VÀNG' : item.id === -2 ? '💎 NGỌC' : '🔴 NGỌC KHÓA') : '📦 Vật Phẩm:'}
                </span>
                ${!isCurrency ? `
                    <div style="flex: 1; min-width: 220px; position: relative;">
                        <input type="text" class="input-field" style="font-size: 12px; padding: 6px 10px; background: #ffffff; font-weight: 700; color: var(--teamobi-orange-dark); border: 1px solid #ffcc80;"
                            placeholder="🔍 Gõ mã ID hoặc tên vật phẩm (VD: 1795)..."
                            value="${item.id > 0 ? '#' + item.id + ' - ' + (item.name || '') : ''}"
                            oninput="filterGcItemSearch(this, ${index})"
                            onfocus="filterGcItemSearch(this, ${index})">
                        <div class="suggestions-dropdown" style="display: none; position: absolute; top: 100%; left: 0; right: 0; z-index: 10000; max-height: 200px; overflow-y: auto; background: #ffffff; border: 1px solid #ffcc80; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.15);"></div>
                    </div>
                ` : `<span style="font-weight: bold; color: var(--text-main);">${item.name || ''}</span>`}
                <div style="display: flex; gap: 4px; align-items: center;">
                    <span style="font-size: 12px; color: var(--text-muted);">Số lượng:</span>
                    <input type="number" class="input-field" style="width: 100px; font-weight: bold; background: #ffffff; color: #1e293b; border: 1px solid #cbd5e1;" value="${item.quantity}" onchange="updateGcItemField(${index}, 'quantity', this.value)">
                </div>
                <button type="button" class="btn-danger" style="padding: 6px 10px;" onclick="removeGcItemRow(${index})">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
            ${optionsHtml}
        `;
        container.appendChild(row);
    });
}

function filterGcItemSearch(inputEl, gcIndex) {
    const val = inputEl.value.trim().toLowerCase();
    const dropdown = inputEl.nextElementSibling;
    if (!dropdown) return;

    if (itemTemplates.length === 0) {
        dropdown.style.display = 'none';
        return;
    }

    let matches = itemTemplates;
    if (val) {
        const cleanVal = val.startsWith('#') ? val.substring(1).trim() : val;
        matches = itemTemplates.filter(i =>
            i.id.toString().includes(cleanVal) ||
            (i.name && i.name.toLowerCase().includes(cleanVal))
        );
    }

    if (!matches || matches.length === 0) {
        dropdown.innerHTML = '<div style="padding: 8px; font-size: 12px; color: var(--text-muted);">❌ Không tìm thấy vật phẩm nào</div>';
        dropdown.style.display = 'block';
        return;
    }

    let html = '';
    matches.slice(0, 20).forEach(t => {
        html += `
            <div style="padding: 8px 10px; font-size: 12px; border-bottom: 1px solid #fff3e0; cursor: pointer; display: flex; align-items: center; justify-content: space-between;"
                 onclick="selectGcItemSearchResult(${gcIndex}, ${t.id}, '${escapeHtml(t.name).replace(/'/g, "\\'")}')">
                <span style="font-weight: 700; color: var(--text-main);">${escapeHtml(t.name)}</span>
                <span style="background: #fff3e0; color: #e65100; padding: 2px 6px; border-radius: 4px; font-weight: 800; font-size: 11px;">#${t.id}</span>
            </div>
        `;
    });
    dropdown.innerHTML = html;
    dropdown.style.display = 'block';
}

function selectGcItemSearchResult(gcIndex, itemId, itemName) {
    if (currentGcItems[gcIndex]) {
        currentGcItems[gcIndex].id = itemId;
        currentGcItems[gcIndex].name = itemName;
        renderGcItemsRows();
    }
}

function filterGcOptionSelect(inputEl, gcIndex, optIdx) {
    const val = inputEl.value.toLowerCase().trim();
    const select = inputEl.nextElementSibling;
    if (!select) return;

    const listToUse = (optionTemplates && optionTemplates.length > 0) ? optionTemplates : POPULAR_OPTIONS;
    const matches = listToUse.filter(o =>
        !val ||
        o.id.toString().includes(val) ||
        (o.name && o.name.toLowerCase().includes(val))
    );

    let html = '';
    if (matches.length === 0) {
        html = '<option value="">❌ Không tìm thấy option nào khớp</option>';
    } else {
        matches.forEach(o => {
            html += `<option value="${o.id}">${o.id} - ${escapeHtml(o.name)}</option>`;
        });
    }

    select.innerHTML = html;
    if (matches.length > 0) {
        const selectedId = matches[0].id;
        select.value = selectedId;
        updateGcOptionField(gcIndex, optIdx, 'id', selectedId);
    }
}

async function handleCreateGiftCode(e) {
    e.preventDefault();
    const code = document.getElementById('gc-code-input').value.trim();
    const countLeft = parseInt(document.getElementById('gc-count-input').value) || 100;
    const expDate = document.getElementById('gc-exp-input').value;

    if (!code) {
        showToast('Vui lòng nhập mã GiftCode!', 'error');
        return;
    }

    if (currentGcItems.length === 0) {
        showToast('Vui lòng thêm ít nhất 1 phần quà vào GiftCode!', 'error');
        return;
    }

    const detailArray = currentGcItems.map(item => ({
        id: item.id,
        quantity: item.quantity,
        options: item.options || []
    }));

    try {
        const resp = await fetch('/api/admin/giftcodes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                code: code,
                countLeft: countLeft,
                dateexpired: expDate || null,
                detail: detailArray
            })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message, 'success');
            currentGcItems = [];
            renderGcItemsRows();
            document.getElementById('create-giftcode-form').reset();
            loadAdminGiftcodes();
        } else {
            showToast(data.message || 'Lỗi khi tạo GiftCode', 'error');
        }
    } catch (err) {
        showToast('Không thể kết nối Server', 'error');
    }
}

async function loadAdminGiftcodes() {
    try {
        const resp = await fetch('/api/admin/giftcodes');
        const rows = await resp.json();

        const badge = document.getElementById('gc-count-badge');
        if (badge) badge.innerText = `${rows.length} Mã`;

        const tbody = document.getElementById('gc-table-body');
        if (!tbody) return;
        tbody.innerHTML = '';

        if (!Array.isArray(rows) || rows.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 20px;">Chưa có mã GiftCode nào được tạo!</td></tr>';
            return;
        }

        rows.forEach(gc => {
            const tr = document.createElement('tr');
            tr.style.borderBottom = '1px solid #f1f5f9';

            let detailStr = '';
            try {
                const detailArr = typeof gc.detail === 'string' ? JSON.parse(gc.detail) : gc.detail;
                if (Array.isArray(detailArr)) {
                    detailStr = detailArr.map(item => {
                        if (item.id === -1) return `<span style="color: var(--gold); font-weight: bold;">💰 ${Number(item.quantity).toLocaleString('vi-VN')} Vàng</span>`;
                        if (item.id === -2) return `<span style="color: var(--cyan); font-weight: bold;">💎 ${Number(item.quantity).toLocaleString('vi-VN')} Ngọc</span>`;
                        if (item.id === -3) return `<span style="color: #f87171; font-weight: bold;">🔴 ${Number(item.quantity).toLocaleString('vi-VN')} Ngọc Khóa</span>`;
                        const temp = itemTemplates.find(t => t.id === item.id);
                        const name = temp ? temp.name : `Item #${item.id}`;
                        return `<span style="color: var(--text-main);">📦 x${item.quantity} ${escapeHtml(name)}</span>`;
                    }).join(', ');
                }
            } catch (e) {
                detailStr = 'Phần quà';
            }

            const expDateStr = gc.expired ? new Date(gc.expired).toLocaleDateString('vi-VN') : 'Không hạn';

            tr.innerHTML = `
                <td data-label="Mã Code" style="padding: 12px; font-weight: 800; color: var(--gold); font-size: 15px;">${escapeHtml(gc.code)}</td>
                <td data-label="Lượt Còn" style="padding: 12px; color: var(--cyan); font-weight: bold;">${gc.count_left === -1 || gc.count_left >= 999999 ? 'Vô hạn' : gc.count_left + ' lượt'}</td>
                <td data-label="Phần Quà" style="padding: 12px; font-size: 13px;">${detailStr}</td>
                <td data-label="Hạn Dùng" style="padding: 12px; color: var(--text-muted); font-size: 12px;">${expDateStr}</td>
                <td data-label="Thao Tác" style="padding: 12px;">
                    <button type="button" class="btn-secondary" style="padding: 4px 8px; font-size: 11px; background: rgba(239, 68, 68, 0.2); color: #f87171;" onclick="deleteAdminGiftcode(${gc.id}, '${escapeHtml(gc.code)}')">
                        <i class="fa-solid fa-trash"></i> Xóa
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('Lỗi load giftcodes:', err);
    }
}

function deleteAdminGiftcode(id, code) {
    customConfirm(
        'XÓA GIFTCODE',
        `Bạn có chắc chắn muốn xóa mã GiftCode <strong style="color: var(--gold);">${code}</strong> không?`,
        async () => {
            try {
                const resp = await fetch(`/api/admin/giftcodes/${id}`, { method: 'DELETE' });
                const data = await resp.json();
                showToast(data.message, data.status === 'success' ? 'success' : 'error');
                loadAdminGiftcodes();
            } catch (e) {
                showToast('Lỗi khi xóa GiftCode', 'error');
            }
        }
    );
}

async function handleUserRedeemGiftcode(e) {
    e.preventDefault();
    if (!currentLoggedUser) {
        showToast('Vui lòng đăng nhập lại!', 'error');
        return;
    }
    if (!currentLoggedUser.hasPlayer) {
        customConfirm(
            'CHƯA CÓ NHÂN VẬT',
            'Tài khoản của bạn <strong style="color: var(--red);">chưa tạo nhân vật</strong> trong game.<br>Vui lòng mở Game Ngọc Rồng, đăng nhập và <strong>tạo nhân vật mới</strong> trước khi nhập mã quà tặng nhé!',
            null
        );
        return;
    }

    const input = document.getElementById('user-gc-input');
    const code = input ? input.value.trim() : '';

    if (!code) {
        showToast('Vui lòng nhập mã GiftCode!', 'error');
        return;
    }

    try {
        const resp = await fetch('/api/user/use-giftcode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerName: currentLoggedUser.player.name,
                code: code
            })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message, 'success');
            if (input) input.value = '';
        } else {
            showToast(data.message || 'Lỗi khi nhập GiftCode', 'error');
        }
    } catch (err) {
        showToast('Không thể kết nối Server Game', 'error');
    }
}

// --- UTILS ---
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icon = type === 'success' ? 'fa-circle-check' : 'fa-circle-exclamation';
    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${escapeHtml(message)}</span>`;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.3s ease-out';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/[&<>"']/g, function (m) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
    });
}

// --- FIX DUPLICATE ITEM OPTIONS ---
async function fixDuplicateItemOptions() {
    if (!confirm('⚠️ Bạn có chắc chắn muốn quét & sửa TẤT CẢ đồ bị trùng chỉ số trên toàn bộ nhân vật?\n\nLưu ý: Nhân vật đang ONLINE cần thoát game và đăng nhập lại để thấy thay đổi.')) {
        return;
    }

    const btn = document.getElementById('btn-fix-duplicate-options');
    const origText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang quét toàn bộ Database...';

    try {
        const resp = await fetch('/api/fix-duplicate-options', { method: 'POST' });
        const data = await resp.json();

        if (data.status === 'success') {
            showToast(data.message, 'success');
        } else {
            showToast(data.message || 'Lỗi khi sửa đồ', 'error');
        }
    } catch (err) {
        showToast('Không thể kết nối API: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = origText;
    }
}

// --- BOT MANAGEMENT LOGIC ---
async function loadBotsList() {
    const tbody = document.getElementById('bots-table-body');
    const badge = document.getElementById('bot-total-badge');
    if (!tbody) return;

    try {
        const resp = await fetch('/api/admin/bots');
        if (!resp.ok) throw new Error('HTTP ' + resp.status);
        const data = await resp.json();

        const bots = data.bots || [];
        const total = data.total || bots.length;

        if (badge) {
            badge.innerHTML = `<i class="fa-solid fa-network-wired"></i> ${total} Bot Đang Chạy`;
        }

        if (bots.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align: center; color: var(--text-muted); padding: 30px;">
                        <i class="fa-solid fa-robot" style="font-size: 32px; display: block; margin-bottom: 10px; opacity: 0.4;"></i>
                        Hiện chưa có con Bot giả lập nào trong Server.<br>
                        Hãy chọn số lượng và bấm <strong>"TẠO BOT NGAY"</strong> ở bên trên!
                    </td>
                </tr>`;
            return;
        }

        const genderNames = ['Trái Đất', 'Namếc', 'Xayda'];
        const genderColors = ['var(--cyan)', 'var(--green)', '#ff4757'];

        tbody.innerHTML = bots.map(b => {
            const genderStr = genderNames[b.gender] || 'Không xác định';
            const genderColor = genderColors[b.gender] || 'var(--gold)';
            const powerFormatted = (b.power || 0).toLocaleString('vi-VN');

            return `
                <tr>
                    <td><code style="color: var(--cyan);">${b.id}</code></td>
                    <td><strong style="color: var(--gold);">${b.name}</strong></td>
                    <td><span style="color: ${genderColor}; font-weight: 600;">${genderStr}</span></td>
                    <td style="font-weight: 700; color: var(--text-main);">${powerFormatted} SM</td>
                    <td><span class="badge" style="background: rgba(255,255,255,0.08);">${b.mapName} (K.${b.zoneId})</span></td>
                    <td>
                        <button type="button" class="btn-secondary" onclick="kickBotFromWeb(${b.id})" style="font-size: 11px; padding: 2px 8px; border-color: #ff4757; color: #ff4757;">
                            <i class="fa-solid fa-user-minus"></i> Kích Bot
                        </button>
                    </td>
                </tr>`;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: #ff4757;">Lỗi tải dữ liệu Bot: ${err.message}</td></tr>`;
    }
}

async function spawnBotsFromWeb() {
    const typeSelect = document.getElementById('bot-type-select');
    const countSelect = document.getElementById('bot-count-select');
    if (!typeSelect || !countSelect) return;

    const type = parseInt(typeSelect.value) || 0;
    const count = parseInt(countSelect.value) || 5;

    try {
        const resp = await fetch('/api/admin/bots', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'spawn', type, count })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message || `Đã tạo ${count} Bot thành công!`, 'success');
            loadBotsList();
        } else {
            showToast(data.message || 'Lỗi khi tạo Bot', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối Server: ' + err.message, 'error');
    }
}

function confirmClearAllBots() {
    if (confirm('⚠️ Bạn có chắc chắn muốn XÓA SẠCH tất cả Bot giả khỏi toàn bộ các Map trong Server?')) {
        clearAllBotsFromWeb();
    }
}

async function clearAllBotsFromWeb() {
    try {
        const resp = await fetch('/api/admin/bots', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'clear' })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message || 'Đã dọn dẹp toàn bộ Bot!', 'success');
            loadBotsList();
        } else {
            showToast(data.message || 'Lỗi khi xóa Bot', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối Server: ' + err.message, 'error');
    }
}

async function kickBotFromWeb(botId) {
    try {
        const resp = await fetch('/api/admin/bots', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'kick_one', botId })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message || `Đã kích Bot ID ${botId}`, 'success');
            loadBotsList();
        } else {
            showToast(data.message || 'Lỗi khi kích Bot', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối Server: ' + err.message, 'error');
    }
}

// --- BOSS MANAGEMENT LOGIC ---
let allBossesData = [];

async function loadBosses() {
    const gridContainer = document.getElementById('boss-grid-container');
    if (!gridContainer) return;

    try {
        const resp = await fetch('/api/admin/bosses');
        if (!resp.ok) throw new Error('Không thể lấy dữ liệu Boss từ Server API');

        allBossesData = await resp.json();
        if (!Array.isArray(allBossesData)) allBossesData = [];

        // Update statistics
        let activeCount = 0;
        let restCount = 0;
        let dieCount = 0;

        allBossesData.forEach(boss => {
            const st = (boss.status || '').toUpperCase();
            if (st === 'ACTIVE' || st === 'JOIN_MAP' || st === 'CHAT_S' || st === 'CHAT_E') {
                activeCount++;
            } else if (st === 'REST' || st === 'RESPAWN') {
                restCount++;
            } else if (st === 'DIE' || st === 'LEAVE_MAP' || st === 'AFK') {
                dieCount++;
            } else {
                activeCount++;
            }
        });

        const totalEl = document.getElementById('boss-stat-total');
        const activeEl = document.getElementById('boss-stat-active');
        const restEl = document.getElementById('boss-stat-rest');
        const dieEl = document.getElementById('boss-stat-die');
        const badgeEl = document.getElementById('boss-total-badge');

        if (totalEl) totalEl.innerText = allBossesData.length;
        if (activeEl) activeEl.innerText = activeCount;
        if (restEl) restEl.innerText = restCount;
        if (dieEl) dieEl.innerText = dieCount;
        if (badgeEl) badgeEl.innerHTML = `<i class="fa-solid fa-crown"></i> ${allBossesData.length} Boss`;

        filterBossesList();
    } catch (err) {
        gridContainer.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: #ff4757; padding: 30px; background: rgba(255, 71, 87, 0.1); border: 1px solid rgba(255, 71, 87, 0.3); border-radius: 12px;">
            <i class="fa-solid fa-server" style="font-size: 32px; margin-bottom: 12px;"></i><br>
            <strong style="font-size: 16px;">Không thể lấy dữ liệu từ Game Server (Port 14446)</strong><br>
            <span style="font-size: 13px; color: var(--text-muted); display: inline-block; margin-top: 6px; max-width: 500px;">Vui lòng đảm bảo Game Server Java đang khởi chạy. (${err.message})</span><br><br>
            <button type="button" class="btn-primary" onclick="loadBosses()" style="font-size: 12px; padding: 8px 20px; background: linear-gradient(135deg, #ff4757, #ff6b81);"><i class="fa-solid fa-arrows-rotate"></i> Thử Lại Ngay</button>
        </div>`;
    }
}

function filterBossesList() {
    const searchInput = document.getElementById('boss-search-input');
    const statusFilter = document.getElementById('boss-status-filter');

    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const filterStatus = statusFilter ? statusFilter.value : 'ALL';

    const filtered = allBossesData.filter(boss => {
        const name = (boss.name || '').toLowerCase();
        const id = String(boss.id || '');
        const mapName = (boss.mapName || '').toLowerCase();
        const st = (boss.status || '').toUpperCase();

        const matchesQuery = !query || name.includes(query) || id.includes(query) || mapName.includes(query);

        let matchesStatus = true;
        if (filterStatus === 'ACTIVE') {
            matchesStatus = (st === 'ACTIVE' || st === 'JOIN_MAP' || st === 'CHAT_S' || st === 'CHAT_E');
        } else if (filterStatus === 'REST') {
            matchesStatus = (st === 'REST' || st === 'RESPAWN');
        } else if (filterStatus === 'DIE') {
            matchesStatus = (st === 'DIE' || st === 'LEAVE_MAP' || st === 'AFK');
        }

        return matchesQuery && matchesStatus;
    });

    renderBosses(filtered);
}

let bossesMap = {};

function renderBosses(bossList) {
    const gridContainer = document.getElementById('boss-grid-container');
    if (!gridContainer) return;

    bossesMap = {};
    if (bossList && Array.isArray(bossList)) {
        bossList.forEach(b => {
            if (b && b.id !== undefined) {
                bossesMap[b.id] = b;
            }
        });
    }

    const spawnCardHtml = `
    <div onclick="openSpawnBossModal()" style="background: rgba(255, 71, 87, 0.08); border: 2px dashed rgba(255, 71, 87, 0.5); border-radius: 14px; padding: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; cursor: pointer; min-height: 220px; transition: all 0.3s ease;" onmouseover="this.style.background='rgba(255, 71, 87, 0.18)'; this.style.borderColor='#ff4757';" onmouseout="this.style.background='rgba(255, 71, 87, 0.08)'; this.style.borderColor='rgba(255, 71, 87, 0.5)';">
        <div style="width: 54px; height: 54px; border-radius: 50%; background: linear-gradient(135deg, #ff4757, #ff6b81); display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; margin-bottom: 12px; box-shadow: 0 4px 15px rgba(255, 71, 87, 0.4);">
            <i class="fa-solid fa-plus"></i>
        </div>
        <h3 style="font-size: 16px; color: #ff4757; margin: 0 0 6px 0; font-weight: 700;">TRIỆU HỒI BOSS MỚI</h3>
        <p style="font-size: 12px; color: var(--text-muted); margin: 0; max-width: 210px; line-height: 1.4;">Bấm vào đây để chọn Boss Săn Đệ Tử, Boss Ma Bư, Beerus,... và Map xuất hiện</p>
    </div>
    `;

    if (!bossList || bossList.length === 0) {
        gridContainer.innerHTML = spawnCardHtml + `<div style="grid-column: 2 / -1; text-align: center; color: var(--text-muted); padding: 40px;">
            <i class="fa-solid fa-ghost" style="font-size: 36px; opacity: 0.5;"></i><br><br>Không có Boss nào phù hợp bộ lọc. Hãy ấn ô bên trái để triệu hồi Boss mới!
        </div>`;
        return;
    }

    gridContainer.innerHTML = spawnCardHtml + bossList.map(boss => {
        const hp = boss.hp || 0;
        const maxHp = boss.maxHp || 1;
        const hpPercent = Math.min(100, Math.max(0, Math.round((hp / maxHp) * 100)));

        let hpColor = '#2ed573';
        if (hpPercent < 20) hpColor = '#ff4757';
        else if (hpPercent < 50) hpColor = '#ffa502';

        const st = (boss.status || 'REST').toUpperCase();
        let statusBadge = '';
        let borderTopColor = '#ff6600';

        if (st === 'ACTIVE' || st === 'JOIN_MAP' || st === 'CHAT_S' || st === 'CHAT_E') {
            statusBadge = `<span class="badge-online" style="background: #dcfce7; color: #15803d; border: 1px solid #86efac; font-size: 11px; padding: 2px 8px; font-weight: 700;"><i class="fa-solid fa-circle-dot"></i> ĐANG SỐNG (${st})</span>`;
            borderTopColor = '#22c55e';
        } else if (st === 'REST' || st === 'RESPAWN') {
            statusBadge = `<span class="badge-online" style="background: #fef9c3; color: #a16207; border: 1px solid #fde047; font-size: 11px; padding: 2px 8px; font-weight: 700;"><i class="fa-solid fa-clock"></i> CHỜ HỒI SINH (${st})</span>`;
            borderTopColor = '#eab308';
        } else {
            statusBadge = `<span class="badge-offline" style="background: #fee2e2; color: #b91c1c; border: 1px solid #fca5a5; font-size: 11px; padding: 2px 8px; font-weight: 700;"><i class="fa-solid fa-skull"></i> ĐÃ CHẾT (${st})</span>`;
            borderTopColor = '#ef4444';
        }

        const iconUrl = boss.avatarUrl || `/icons/${boss.iconId || boss.head || 0}.png`;
        const fallbackUrl = `/icons/${boss.head || 0}.png`;

        return `
        <div style="background: #ffffff; border: 1px solid #ffe0b2; border-top: 4px solid ${borderTopColor}; border-radius: 12px; padding: 16px; display: flex; flex-direction: column; justify-content: space-between; transition: all 0.2s ease; box-shadow: 0 4px 15px rgba(230, 81, 0, 0.06);">
            <div>
                <!-- AVATAR & NAME ROW -->
                <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 12px;">
                    <div style="width: 52px; height: 52px; border-radius: 10px; background: var(--teamobi-orange-bg); border: 1px solid var(--teamobi-orange-border); display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0;">
                        <img src="${iconUrl}" onerror="this.onerror=null; this.src='${fallbackUrl}'; this.onerror=function(){this.src='https://cdn-icons-png.flaticon.com/512/1144/1144760.png';}" style="width: 42px; height: 42px; object-fit: contain;">
                    </div>
                    <div style="flex: 1; overflow: hidden;">
                        <div style="display: flex; align-items: center; justify-content: space-between; gap: 6px;">
                            <h3 style="font-size: 15px; color: var(--text-main); margin: 0; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${boss.name || 'Boss'}</h3>
                            <span style="font-size: 11px; background: #f1f5f9; color: var(--text-muted); padding: 2px 6px; border-radius: 4px; font-weight: 700;">#${boss.id}</span>
                        </div>
                        <div style="margin-top: 4px;">${statusBadge}</div>
                    </div>
                </div>

                <!-- HEALTH BAR -->
                <div style="margin-bottom: 12px;">
                    <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); font-weight: 700; margin-bottom: 4px;">
                        <span><i class="fa-solid fa-heart" style="color: #ef4444;"></i> HP (${hpPercent}%)</span>
                        <span style="color: #1e293b; font-weight: 800;">${hp.toLocaleString('vi-VN')} / ${maxHp.toLocaleString('vi-VN')}</span>
                    </div>
                    <div style="width: 100%; height: 8px; background: #f1f5f9; border-radius: 4px; overflow: hidden; border: 1px solid #e2e8f0;">
                        <div style="width: ${hpPercent}%; height: 100%; background: ${hpColor}; transition: width 0.4s ease; border-radius: 4px;"></div>
                    </div>
                </div>

                <!-- STATS INFO ROW (DAME & DEF) -->
                <div style="display: flex; justify-content: space-between; font-size: 12px; color: var(--text-muted); background: #fff8f3; padding: 8px 10px; border-radius: 8px; margin-bottom: 10px; border: 1px solid #ffe0b2;">
                    <span><i class="fa-solid fa-hand-fist" style="color: #e65100;"></i> Đánh: <strong style="color: #1e293b;">${(boss.dame || 0).toLocaleString('vi-VN')}</strong></span>
                    <span><i class="fa-solid fa-shield-halved" style="color: #0284c7;"></i> Giáp: <strong style="color: #1e293b;">${(boss.def || 0).toLocaleString('vi-VN')}</strong></span>
                </div>

                <!-- LOCATION INFO -->
                <div style="background: #f8fafc; border-radius: 8px; padding: 8px 12px; font-size: 12px; color: var(--text-main); margin-bottom: 14px; border: 1px solid #e2e8f0;">
                    <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 4px;">
                        <i class="fa-solid fa-map-location-dot" style="color: #0284c7;"></i>
                        <strong style="color: var(--text-main); font-weight: 800;">${boss.mapName || 'Chưa xuất hiện'}</strong>
                        ${boss.mapId >= 0 ? `<span style="color: var(--text-muted); font-size: 11px;">(${boss.mapId})</span>` : ''}
                    </div>
                    <div style="display: flex; justify-content: space-between; color: var(--text-muted); font-size: 11px;">
                        <span>Khu vực: <strong style="color: var(--teamobi-orange-dark);">${boss.zoneId >= 0 ? boss.zoneId : '-'}</strong></span>
                        <span>Tọa độ: <strong style="color: #1e293b;">X: ${boss.x || 0}, Y: ${boss.y || 0}</strong></span>
                    </div>
                </div>
            </div>

            <!-- ACTION BUTTONS -->
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-bottom: 6px;">
                <button type="button" class="btn-primary" onclick="handleBossAction('respawn', ${boss.id})" style="padding: 6px 4px; font-size: 11px; background: linear-gradient(135deg, #16a34a, #059669); color: #fff; font-weight: 700;" title="Hồi sinh Boss & kích hoạt ngay">
                    <i class="fa-solid fa-bolt"></i> Hồi Sinh
                </button>
                <button type="button" class="btn-secondary" onclick="openEditBossStatsModal(${boss.id})" style="padding: 6px 4px; font-size: 11px; border-color: #0284c7; color: #0284c7; font-weight: 700;" title="Điều chỉnh HP / Max HP / Dame">
                    <i class="fa-solid fa-sliders"></i> Chỉ Số
                </button>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px;">
                <button type="button" class="btn-secondary" onclick="handleBossAction('kill', ${boss.id})" style="padding: 6px 4px; font-size: 11px; border-color: #d97706; color: #d97706; font-weight: 700;" title="Tiêu diệt Boss trong game">
                    <i class="fa-solid fa-skull"></i> Tiêu Diệt
                </button>
                <button type="button" class="btn-secondary" onclick="confirmDeleteBoss(${boss.id}, '${escapeHtml(boss.name || 'Boss')}')" style="padding: 6px 4px; font-size: 11px; border-color: #ef4444; color: #dc2626; background: #fef2f2; font-weight: 700;" title="Xóa Boss khỏi danh sách">
                    <i class="fa-solid fa-trash"></i> Xóa Boss
                </button>
            </div>
        </div>
        `;
    }).join('');
}

function confirmDeleteBoss(bossId, bossName) {
    customConfirm('XÓA BOSS KHỎI HỆ THỐNG', `Bạn có chắc chắn muốn xóa Boss <strong>${bossName}</strong> (ID: ${bossId}) khỏi danh sách?`, () => {
        handleBossAction('delete', bossId);
    });
}

let currentEditBossId = null;

function openEditBossStatsModal(bossId) {
    currentEditBossId = bossId;
    const boss = bossesMap[bossId];

    const nameEl = document.getElementById('edit-boss-name');
    const idEl = document.getElementById('edit-boss-id');
    const hpInput = document.getElementById('edit-boss-hp-input');
    const maxHpInput = document.getElementById('edit-boss-maxhp-input');
    const dameInput = document.getElementById('edit-boss-dame-input');
    const defInput = document.getElementById('edit-boss-def-input');

    if (nameEl) nameEl.innerText = boss ? (boss.name || 'Boss') : ('Boss ID: ' + bossId);
    if (idEl) idEl.innerText = bossId;
    if (hpInput) hpInput.value = (boss && boss.hp !== undefined) ? boss.hp : '';
    if (maxHpInput) maxHpInput.value = (boss && boss.maxHp !== undefined) ? boss.maxHp : '';
    if (dameInput) dameInput.value = (boss && boss.dame !== undefined) ? boss.dame : '';
    if (defInput) defInput.value = (boss && boss.def !== undefined) ? boss.def : '';

    showModal('modal-edit-boss-stats');
}

async function submitEditBossStats() {
    if (currentEditBossId === null || currentEditBossId === undefined) return;

    const hpInput = document.getElementById('edit-boss-hp-input');
    const maxHpInput = document.getElementById('edit-boss-maxhp-input');
    const dameInput = document.getElementById('edit-boss-dame-input');
    const defInput = document.getElementById('edit-boss-def-input');

    let hp = hpInput && hpInput.value.trim() !== '' ? parseInt(hpInput.value.trim()) : -1;
    let maxHp = maxHpInput && maxHpInput.value.trim() !== '' ? parseInt(maxHpInput.value.trim()) : -1;
    let dame = dameInput && dameInput.value.trim() !== '' ? parseInt(dameInput.value.trim()) : -1;
    let def = defInput && defInput.value.trim() !== '' ? parseInt(defInput.value.trim()) : -1;

    if (hp > 2000000000) hp = 2000000000;
    if (maxHp > 2000000000) maxHp = 2000000000;
    if (dame > 2000000000) dame = 2000000000;
    if (def > 2000000000) def = 2000000000;

    try {
        const resp = await fetch('/api/admin/bosses/action', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'set_stats', bossId: currentEditBossId, hp, maxHp, dame, def })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message || 'Cập nhật chỉ số Boss thành công!', 'success');
            closeModal('modal-edit-boss-stats');
            loadBosses();
        } else {
            showToast(data.message || 'Cập nhật thất bại', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối: ' + err.message, 'error');
    }
}

async function handleBossAction(action, bossId) {
    try {
        const resp = await fetch('/api/admin/bosses/action', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action, bossId })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message || 'Thao tác Boss thành công', 'success');
            loadBosses();
        } else {
            showToast(data.message || 'Thao tác thất bại', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối: ' + err.message, 'error');
    }
}

function openSpawnBossModal() {
    const modal = document.getElementById('modal-spawn-boss');
    if (modal) modal.style.display = 'flex';
}

function closeSpawnBossModal() {
    const modal = document.getElementById('modal-spawn-boss');
    if (modal) modal.style.display = 'none';
}

function onSelectPresetBoss() {
    const select = document.getElementById('preset-boss-select');
    const customInput = document.getElementById('custom-boss-id-input');
    if (select && customInput && select.value) {
        customInput.value = select.value;
    }
}

async function submitSpawnBoss() {
    const customBossInput = document.getElementById('custom-boss-id-input');
    const mapSelect = document.getElementById('spawn-map-select');
    const customHpInput = document.getElementById('custom-boss-hp-input');
    const customDameInput = document.getElementById('custom-boss-dame-input');
    const customDefInput = document.getElementById('custom-boss-def-input');

    const bossId = customBossInput ? parseInt(customBossInput.value) : null;
    if (!bossId || isNaN(bossId)) {
        return showToast('Vui lòng chọn hoặc nhập Boss ID hợp lệ!', 'error');
    }

    let mapId = mapSelect ? parseInt(mapSelect.value) : -1;

    let hp = customHpInput && customHpInput.value.trim() !== '' ? parseInt(customHpInput.value.trim()) : 0;
    let dame = customDameInput && customDameInput.value.trim() !== '' ? parseInt(customDameInput.value.trim()) : 0;
    let def = customDefInput && customDefInput.value.trim() !== '' ? parseInt(customDefInput.value.trim()) : 0;

    if (hp > 2000000000) {
        hp = 2000000000;
        showToast('Giới hạn Máu tối đa của Game NRO là 2 tỷ HP (2.000.000.000). Hệ thống đã tự động đặt 2 tỷ HP!', 'warning');
    }
    if (dame > 2000000000) {
        dame = 2000000000;
        showToast('Giới hạn Sức Đánh tối đa của Game NRO là 2 tỷ. Hệ thống đã tự động đặt 2 tỷ Dame!', 'warning');
    }
    if (def > 2000000000) {
        def = 2000000000;
        showToast('Giới hạn Giáp tối đa của Game NRO là 2 tỷ. Hệ thống đã tự động đặt 2 tỷ Giáp!', 'warning');
    }

    try {
        const resp = await fetch('/api/admin/bosses/spawn', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bossId, mapId, hp, dame, def })
        });
        const data = await resp.json();
        if (data.status === 'success') {
            showToast(data.message || 'Triệu hồi Boss thành công!', 'success');
            closeModal('modal-spawn-boss');
            loadBosses();
        } else {
            showToast(data.message || 'Triệu hồi thất bại', 'error');
        }
    } catch (err) {
        showToast('Lỗi kết nối: ' + err.message, 'error');
    }
}

// --- PLAYER INVENTORY INSPECTOR ---
let currentInventoryData = null;
let currentInvSubTab = 'body';

async function openPlayerInventoryModal(playerName) {
    if (!playerName) return;
    openModal('modal-player-inventory');
    document.getElementById('inv-modal-player-name').innerText = playerName;
    document.getElementById('inv-items-container').innerHTML = '<p style="color: var(--text-muted); text-align: center; grid-column: 1/-1; padding: 40px;"><i class="fa-solid fa-spinner fa-spin" style="font-size: 24px; margin-bottom: 8px; display: block; color: var(--teamobi-orange);"></i>Đang nạp hành trang nhân vật ' + escapeHtml(playerName) + '...</p>';

    try {
        const resp = await fetch(`/api/player-inventory?playerName=${encodeURIComponent(playerName)}`);
        const data = await resp.json();

        if (data.status !== 'success') {
            showToast(data.message || 'Không thể tải hành trang', 'error');
            closeModal('modal-player-inventory');
            return;
        }

        currentInventoryData = data;
        document.getElementById('inv-gold-display').innerText = (data.gold || 0).toLocaleString('vi-VN');
        document.getElementById('inv-gem-display').innerText = (data.gem || 0).toLocaleString('vi-VN');
        document.getElementById('inv-ruby-display').innerText = (data.ruby || 0).toLocaleString('vi-VN');

        const badge = document.getElementById('inv-status-badge');
        if (data.isOnline) {
            badge.className = 'badge-online';
            badge.innerText = 'ONLINE';
        } else {
            badge.className = 'badge-offline';
            badge.innerText = 'OFFLINE';
        }

        document.getElementById('inv-count-body').innerText = (data.itemsBody || []).length;
        document.getElementById('inv-count-bag').innerText = (data.itemsBag || []).length;
        document.getElementById('inv-count-box').innerText = (data.itemsBox || []).length;
        const petCountEl = document.getElementById('inv-count-pet');
        if (petCountEl) {
            petCountEl.innerText = data.hasPet ? (data.itemsPetBody || []).length : 'Chưa có';
        }

        switchInvSubTab('body');

    } catch (err) {
        showToast('Lỗi kết nối khi tải hành trang: ' + err.message, 'error');
    }
}

function switchInvSubTab(tabName) {
    currentInvSubTab = tabName;
    const btnBody = document.getElementById('inv-tab-btn-body');
    const btnBag = document.getElementById('inv-tab-btn-bag');
    const btnBox = document.getElementById('inv-tab-btn-box');
    const btnPet = document.getElementById('inv-tab-btn-pet');

    const activeStyle = 'padding: 6px 12px; font-size: 12px; font-weight: 800; background: var(--teamobi-orange); color: #fff; border-color: var(--teamobi-orange);';
    const inactiveStyle = 'padding: 6px 12px; font-size: 12px; font-weight: 700; background: #ffffff; color: var(--text-main); border: 1px solid #cbd5e1;';

    if (btnBody) btnBody.style.cssText = tabName === 'body' ? activeStyle : inactiveStyle;
    if (btnBag) btnBag.style.cssText = tabName === 'bag' ? activeStyle : inactiveStyle;
    if (btnBox) btnBox.style.cssText = tabName === 'box' ? activeStyle : inactiveStyle;
    if (btnPet) btnPet.style.cssText = tabName === 'pet' ? activeStyle : inactiveStyle;

    renderInvItemsGrid();
}

function renderInvItemsGrid() {
    const container = document.getElementById('inv-items-container');
    if (!container || !currentInventoryData) return;

    let items = [];
    if (currentInvSubTab === 'body') items = currentInventoryData.itemsBody || [];
    else if (currentInvSubTab === 'bag') items = currentInventoryData.itemsBag || [];
    else if (currentInvSubTab === 'box') items = currentInventoryData.itemsBox || [];
    else if (currentInvSubTab === 'pet') items = currentInventoryData.itemsPetBody || [];

    if (currentInvSubTab === 'pet' && !currentInventoryData.hasPet) {
        container.innerHTML = '<p style="color: var(--text-muted); text-align: center; grid-column: 1/-1; padding: 40px; border: 1px dashed #ffe0b2; border-radius: 10px;"><i class="fa-solid fa-user-slash" style="font-size: 28px; margin-bottom: 8px; display: block; color: var(--gold);"></i>Nhân vật này chưa có đệ tử</p>';
        return;
    }

    if (items.length === 0) {
        container.innerHTML = '<p style="color: var(--text-muted); text-align: center; grid-column: 1/-1; padding: 40px; border: 1px dashed #ffe0b2; border-radius: 10px;"><i class="fa-solid fa-box-open" style="font-size: 28px; margin-bottom: 8px; display: block; color: var(--gold);"></i>Không có vật phẩm nào trong mục này</p>';
        return;
    }

    container.innerHTML = '';
    items.forEach(item => {
        const card = document.createElement('div');
        card.style.cssText = 'background: #ffffff; border: 1px solid #ffe0b2; border-radius: 10px; padding: 12px; display: flex; flex-direction: column; justify-content: space-between; box-shadow: 0 2px 6px rgba(0,0,0,0.03); border-top: 3px solid var(--teamobi-orange);';

        const iconId = item.iconID || item.id;
        let optionsHtml = '';
        if (item.options && item.options.length > 0) {
            optionsHtml = `
                <div style="margin-top: 8px; border-top: 1px dashed #ffe0b2; padding-top: 6px; font-size: 11px;">
                    ${item.options.map(o => `
                        <div style="color: #0284c7; font-weight: 600; display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
                            <span>🔹 ${escapeHtml(o.name)}</span>
                            <strong style="color: #0369a1;">+${o.param}</strong>
                        </div>
                    `).join('')}
                </div>
            `;
        }

        const slotBadge = item.slotName ? `<span style="background: #fff3e0; color: #e65100; font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 4px; border: 1px solid #ffb74d;">${escapeHtml(item.slotName)}</span>` : '';

        card.innerHTML = `
            <div>
                <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 6px;">
                    <img src="/icons/${iconId}.png" onerror="this.src='/icons/${item.id}.png'; this.onerror=function(){this.src='/icons/521.png';};" style="width: 36px; height: 36px; object-fit: contain; background: #fff8f3; border-radius: 8px; padding: 2px; border: 1px solid #ffe0b2;" />
                    <div style="flex: 1; overflow: hidden;">
                        <h4 style="font-size: 13px; font-weight: 800; color: var(--text-main); margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(item.name)}</h4>
                        <div style="display: flex; gap: 6px; align-items: center; margin-top: 2px;">
                            <span style="font-size: 11px; color: var(--gold); font-weight: 700;">#${item.id}</span>
                            ${slotBadge}
                        </div>
                    </div>
                </div>
                <div style="display: flex; justify-content: space-between; align-items: center; font-size: 11px; color: var(--text-muted);">
                    <span>Số lượng: <strong style="color: var(--teamobi-orange-dark); font-size: 12px;">x${item.quantity || 1}</strong></span>
                </div>
                ${optionsHtml}
            </div>
        `;
        container.appendChild(card);
    });
}

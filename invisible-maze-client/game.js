// --- Socket & Global State ---
const socket = new WebSocket("ws://localhost:8085/game");
const statusText = document.getElementById("status");

let currentPos = { r: 0, c: 0 };
let targetPos = { r: 0, c: 0 };
let playerGroup = null; 
let leftLeg = null, rightLeg = null;
let leftArm = null, rightArm = null;
let myRole = "";
let globalRoundCounter = parseInt(sessionStorage.getItem("mazeStage")) || 1;
let walkCycle = 0;

socket.onopen = () => {
    statusText.innerText = "Connected to Server. Ready.";
    
    const savedCode = sessionStorage.getItem("mazeRoomCode");
    if (savedCode) {
        statusText.innerText = "Attempting reconnection to room " + savedCode + "...";
        socket.send("JOIN|" + savedCode);
    }
};

document.getElementById("btn-create").onclick = () => socket.send("CREATE");

document.getElementById("btn-join").onclick = () => {
    const code = document.getElementById("input-code").value.trim().toUpperCase();
    if (code.length === 4) {
        sessionStorage.setItem("mazeRoomCode", code);
        socket.send("JOIN|" + code);
    }
};

socket.onmessage = (event) => {
    const msg = event.data;

    if (msg.startsWith("ROOM_CREATED|")) {
        const code = msg.split("|")[1];
        sessionStorage.setItem("mazeRoomCode", code);
        statusText.innerText = "Room Created: " + code + ". Waiting for Explorer...";
    } else if (msg.startsWith("ROOM_JOINED|")) {
        const code = msg.split("|")[1];
        statusText.innerText = "Joined Room: " + code + ". Waiting to start...";
    } else if (msg.startsWith("ERROR|")) {
        const error = msg.split("|")[1];
        statusText.innerText = "Error: " + error;
    } else if (msg.startsWith("LOBBY_STATUS:")) {
        const innerStatus = msg.replace("LOBBY_STATUS:", "");
        statusText.innerText = innerStatus;
        if (innerStatus.includes("CONGRATULATIONS")) {
            document.getElementById("lobby").style.display = "block";
            document.getElementById("hud").style.display = "none";
            document.getElementById("nav-controls-hint").style.display = "none";
            sessionStorage.clear();
        }
    } else if (msg.startsWith("START_GAME|")) {
        const parts = msg.split("|");
        myRole = parts[1];
        const matrix = JSON.parse(parts[2]);

        document.getElementById("lobby").style.display = "none";
        document.getElementById("hud").style.display = "block";
        document.getElementById("hud-role").innerText = myRole;
        document.getElementById("hud-stage").innerText = globalRoundCounter + "/10";

        if (myRole === "NAVIGATOR") {
            document.getElementById("nav-controls-hint").style.display = "block";
        }

        const existingCanvas = document.querySelector('canvas');
        if (existingCanvas) {
            existingCanvas.remove();
        }

        build3DWorld(myRole, matrix);
    } else if (msg.startsWith("PLAYER_MOVED|")) {
        const parts = msg.split("|");
        targetPos.r = parseInt(parts[1]);
        targetPos.c = parseInt(parts[2]);
    } else if (msg.startsWith("STAGE_UPDATE:")) {
        const backendStage = msg.split(":")[1];
        globalRoundCounter = parseInt(backendStage);
        sessionStorage.setItem("mazeStage", globalRoundCounter);
        document.getElementById("hud-stage").innerText = globalRoundCounter + "/10";
    }
};

// --- Texture Generation ---
function createPathTexture() {
    const canvas = document.createElement('canvas');
    canvas.width = 512; canvas.height = 512;
    const ctx = canvas.getContext('2d');
    
    ctx.fillStyle = '#324222';
    ctx.fillRect(0, 0, 512, 512);

    ctx.strokeStyle = '#212d16';
    ctx.lineWidth = 4;
    const tileSize = 64;
    
    for (let x = 0; x <= 512; x += tileSize) {
        for (let y = 0; y <= 512; y += tileSize) {
            ctx.strokeRect(x, y, tileSize, tileSize);
            const tone = Math.floor(Math.random() * 35);
            ctx.fillStyle = `rgb(${50 + tone}, ${65 + tone}, ${35 + tone})`;
            ctx.fillRect(x + 2, y + 2, tileSize - 4, tileSize - 4);
        }
    }
    const texture = new THREE.CanvasTexture(canvas);
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    return texture;
}

function createLeafTexture() {
    const canvas = document.createElement('canvas');
    canvas.width = 256; canvas.height = 256;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#16330f';
    ctx.fillRect(0, 0, 256, 256);

    for (let i = 0; i < 3500; i++) {
        const x = Math.random() * 256;
        const y = Math.random() * 256;
        const g = Math.floor(Math.random() * 110) + 50;
        ctx.fillStyle = `rgb(${10 + Math.floor(g/4)}, ${g}, ${15})`;
        ctx.fillRect(x, y, 3, 3);
    }
    const texture = new THREE.CanvasTexture(canvas);
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    return texture;
}

// --- Detailed Humanoid Character Builder ---
function createHumanoidCharacter() {
    const group = new THREE.Group();

    const skinMat = new THREE.MeshStandardMaterial({ color: 0xffdbac, roughness: 0.6 });
    const shirtMat = new THREE.MeshStandardMaterial({ color: 0x2980b9, roughness: 0.5 });
    const pantsMat = new THREE.MeshStandardMaterial({ color: 0x2c3e50, roughness: 0.7 });
    const bootMat = new THREE.MeshStandardMaterial({ color: 0x1a0f07, roughness: 0.4 });
    const hairMat = new THREE.MeshStandardMaterial({ color: 0x4a2e12, roughness: 0.8 });

    const torso = new THREE.Mesh(new THREE.BoxGeometry(0.38, 0.45, 0.22), shirtMat);
    torso.position.y = 0.55;
    torso.castShadow = true;
    group.add(torso);

    const belt = new THREE.Mesh(new THREE.BoxGeometry(0.40, 0.06, 0.24), bootMat);
    belt.position.y = 0.34;
    group.add(belt);

    const head = new THREE.Mesh(new THREE.SphereGeometry(0.14, 16, 16), skinMat);
    head.position.y = 0.88;
    head.castShadow = true;
    group.add(head);

    const hair = new THREE.Mesh(new THREE.SphereGeometry(0.15, 12, 12, 0, Math.PI * 2, 0, Math.PI / 2), hairMat);
    hair.position.y = 0.90;
    group.add(hair);

    const createLeg = (xOffset) => {
        const legGroup = new THREE.Group();
        legGroup.position.set(xOffset, 0.32, 0);

        const legMesh = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.28, 0.12), pantsMat);
        legMesh.position.y = -0.14;
        legMesh.castShadow = true;
        legGroup.add(legMesh);

        const boot = new THREE.Mesh(new THREE.BoxGeometry(0.13, 0.12, 0.18), bootMat);
        boot.position.set(0, -0.24, 0.03);
        boot.castShadow = true;
        legGroup.add(boot);

        return legGroup;
    };

    leftLeg = createLeg(-0.11);
    rightLeg = createLeg(0.11);
    group.add(leftLeg);
    group.add(rightLeg);

    const createArm = (xOffset) => {
        const armGroup = new THREE.Group();
        armGroup.position.set(xOffset, 0.72, 0);

        const sleeve = new THREE.Mesh(new THREE.BoxGeometry(0.10, 0.30, 0.10), shirtMat);
        sleeve.position.y = -0.15;
        sleeve.castShadow = true;
        armGroup.add(sleeve);

        const hand = new THREE.Mesh(new THREE.SphereGeometry(0.05, 8, 8), skinMat);
        hand.position.y = -0.32;
        armGroup.add(hand);

        return armGroup;
    };

    leftArm = createArm(-0.24);
    rightArm = createArm(0.24);
    group.add(leftArm);
    group.add(rightArm);

    return group;
}

// --- World Builder ---
function build3DWorld(role, matrix) {
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x87CEEB);
    scene.fog = new THREE.FogExp2(0x87CEEB, 0.012);

    const camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 1000);
    const renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    document.body.appendChild(renderer.domElement);

    const mapRows = matrix.length;
    const mapCols = matrix[0].length;
    const centerR = mapRows / 2 - 0.5;
    const centerC = mapCols / 2 - 0.5;

    // Lighting
    const ambientLight = new THREE.AmbientLight(0xdff0ff, 0.65);
    scene.add(ambientLight);

    const sunLight = new THREE.DirectionalLight(0xfffaed, 1.25);
    sunLight.position.set(centerC - 10, 24, centerR - 10);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    sunLight.shadow.bias = -0.0005;
    scene.add(sunLight);

    // Floor
    const pathTex = createPathTexture();
    pathTex.repeat.set(mapCols / 2, mapRows / 2);
    const floorGeo = new THREE.PlaneGeometry(mapCols * 3, mapRows * 3);
    const floorMat = new THREE.MeshStandardMaterial({ map: pathTex, roughness: 0.85 });
    const floor = new THREE.Mesh(floorGeo, floorMat);
    floor.rotation.x = -Math.PI / 2;
    floor.position.set(centerC, 0, centerR);
    floor.receiveShadow = true;
    scene.add(floor);

    // Realistic Hedge Materials & Volumetric Mesh Construction
    const leafTex = createLeafTexture();
    const hedgeMatVisible = new THREE.MeshStandardMaterial({ map: leafTex, roughness: 0.85 });
    const hedgeMatInvisible = new THREE.MeshStandardMaterial({ color: 0x2e5c1e, transparent: true, opacity: 0.02 });

    for (let r = 0; r < mapRows; r++) {
        for (let c = 0; c < mapCols; c++) {
            if (matrix[r][c] === 2) {
                currentPos.r = r; currentPos.c = c;
                targetPos.r = r; targetPos.c = c;
            }
        }
    }

    // Build Detailed Volumetric Realistic Hedges
    for (let r = 0; r < mapRows; r++) {
        for (let c = 0; c < mapCols; c++) {
            const cell = matrix[r][c];

            if (cell === 1) {
                const isNav = (role === "NAVIGATOR");
                const mat = isNav ? hedgeMatVisible : hedgeMatInvisible;
                const hedgeGroup = new THREE.Group();

                // Core Main Hedge Pillar
                const height = 2.2 + (Math.random() * 0.15 - 0.07);
                const wallMesh = new THREE.Mesh(new THREE.BoxGeometry(0.96, height, 0.96), mat);
                wallMesh.position.set(c, height / 2, r);
                wallMesh.castShadow = isNav;
                wallMesh.receiveShadow = isNav;
                hedgeGroup.add(wallMesh);

                // Volumetric Foliage Bump Caps for organic trim look
                if (isNav) {
                    const topCap = new THREE.Mesh(new THREE.BoxGeometry(1.04, 0.2, 1.04), mat);
                    topCap.position.set(c, height + 0.05, r);
                    hedgeGroup.add(topCap);

                    const midBuldge = new THREE.Mesh(new THREE.BoxGeometry(1.02, 0.8, 1.02), mat);
                    midBuldge.position.set(c, height / 2, r);
                    hedgeGroup.add(midBuldge);
                }

                scene.add(hedgeGroup);
            } else if (cell === 2) {
                const pad = new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.03, 0.9), new THREE.MeshStandardMaterial({ color: 0x7f8c8d, roughness: 0.5 }));
                pad.position.set(c, 0.015, r);
                pad.receiveShadow = true;
                scene.add(pad);
            } else if (cell === 3) {
                const fountainGroup = new THREE.Group();

                const pedestal = new THREE.Mesh(
                    new THREE.CylinderGeometry(0.45, 0.5, 0.3, 16),
                    new THREE.MeshStandardMaterial({ color: 0x95a5a6, roughness: 0.4 })
                );
                pedestal.position.set(c, 0.15, r);
                pedestal.castShadow = true;
                fountainGroup.add(pedestal);

                const water = new THREE.Mesh(
                    new THREE.CylinderGeometry(0.4, 0.4, 0.05, 16),
                    new THREE.MeshStandardMaterial({ color: 0x00bfff, roughness: 0.1, metalness: 0.9 })
                );
                water.position.set(c, 0.3, r);
                fountainGroup.add(water);

                const lightBeam = new THREE.PointLight(0x00ffff, 1.5, 3);
                lightBeam.position.set(c, 0.8, r);
                fountainGroup.add(lightBeam);

                scene.add(fountainGroup);
            }
        }
    }

    // Attach Character
    playerGroup = createHumanoidCharacter();
    playerGroup.position.set(currentPos.c, 0, currentPos.r);
    scene.add(playerGroup);

    // Camera and Navigation Controls
    let controls = null;
    if (role === "NAVIGATOR") {
        camera.position.set(centerC, mapCols * 1.1, centerR + mapRows * 0.5);
        
        controls = new THREE.OrbitControls(camera, renderer.domElement);
        controls.target.set(centerC, 0, centerR);
        
        // Enabled Panning (Up/Down/Left/Right), Zooming, and Orbiting
        controls.enablePan = true;
        controls.screenSpacePanning = true; 
        controls.enableDamping = true;
        controls.dampingFactor = 0.08;
        controls.maxPolarAngle = Math.PI / 2.2;
        controls.minDistance = 3;
        controls.maxDistance = mapCols * 2.5;

        // Custom Mouse Setup for Easy Panning
        controls.mouseButtons = {
            LEFT: THREE.MOUSE.PAN,      // Left-Click Drag: Move Up, Down, Left, Right
            MIDDLE: THREE.MOUSE.DOLLY,  // Middle Scroll: Zoom
            RIGHT: THREE.MOUSE.ROTATE   // Right-Click Drag: Rotate camera perspective
        };
        
        renderer.domElement.addEventListener('contextmenu', (e) => e.preventDefault());
    } else {
        camera.position.set(currentPos.c, 4, currentPos.r + 3); 
        camera.lookAt(currentPos.c, 0.5, currentPos.r);
    }

    // Input Movement
    window.addEventListener('keydown', (e) => {
        if (myRole !== "EXPLORER") return;

        let nextR = targetPos.r;
        let nextC = targetPos.c;

        if (e.key === 'w' || e.key === 'ArrowUp') nextR--;
        if (e.key === 's' || e.key === 'ArrowDown') nextR++;
        if (e.key === 'a' || e.key === 'ArrowLeft') nextC--;
        if (e.key === 'd' || e.key === 'ArrowRight') nextC++;

        if (nextR !== targetPos.r || nextC !== targetPos.c) {
            socket.send("MOVE|" + nextR + "|" + nextC);
        }
    });

    // Render & Animation Loop
    function renderFrame() {
        requestAnimationFrame(renderFrame);

        const diffC = targetPos.c - currentPos.c;
        const diffR = targetPos.r - currentPos.r;
        const isMoving = Math.abs(diffC) > 0.01 || Math.abs(diffR) > 0.01;

        currentPos.c += diffC * 0.15;
        currentPos.r += diffR * 0.15;
        
        if (playerGroup) {
            playerGroup.position.set(currentPos.c, 0, currentPos.r);

            if (isMoving) {
                playerGroup.rotation.y = Math.atan2(diffC, diffR);

                walkCycle += 0.25;
                const swing = Math.sin(walkCycle) * 0.6;
                if (leftLeg) leftLeg.rotation.x = swing;
                if (rightLeg) rightLeg.rotation.x = -swing;
                if (leftArm) leftArm.rotation.x = -swing * 0.8;
                if (rightArm) rightArm.rotation.x = swing * 0.8;
            } else {
                if (leftLeg) leftLeg.rotation.x = 0;
                if (rightLeg) rightLeg.rotation.x = 0;
                if (leftArm) leftArm.rotation.x = 0;
                if (rightArm) rightArm.rotation.x = 0;
            }
        }

        if (role === "NAVIGATOR") {
            if (controls) controls.update();
        } else {
            camera.position.x += (currentPos.c - camera.position.x) * 0.08;
            camera.position.z += ((currentPos.r + 3.5) - camera.position.z) * 0.08;
            camera.position.y = 4.5;
            camera.lookAt(currentPos.c, 0.6, currentPos.r);
        }

        renderer.render(scene, camera);
    }
    renderFrame();

    window.addEventListener('resize', () => {
        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(window.innerWidth, window.innerHeight);
    });
}
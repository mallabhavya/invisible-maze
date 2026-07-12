# Invisible Maze

A real-time 2-player web game built to explore asymmetric multiplayer communication in 3D. One player sees everything; the other sees nothing but darkness. Success depends entirely on how well they talk to each other.

## Concept

Two players connect to the same maze, but experience it completely differently:

- **The Navigator** sees the entire maze from above — full layout, trap locations, and the exit, rendered as a top-down 3D map.
- **The Explorer** is inside the maze, moving through near-total darkness with only a few feet of visibility around them.

The Navigator guides the Explorer to the exit before the timer runs out, avoiding traps along the way — using nothing but voice (or text) communication. No map is ever shared with the Explorer, and no visual hints leak between the two views. The fun comes from miscommunication: "go left" meaning different things to different people under time pressure.

## Why this exists

This project was built as a portfolio/capstone piece to demonstrate real-time multiplayer systems design — state synchronization, server-authoritative game logic, and role-based data visibility — using a distinctive, non-generic game concept rather than a standard clone.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Spring WebSocket |
| Frontend | HTML / three.js |
| Communication | Raw WebSocket (JSON messages), no message broker |
| Data storage | In-memory only — no database |

No database is used. Game rooms are ephemeral: created when a Navigator starts a room, and discarded the moment both players disconnect.

## Architecture

Both clients connect to a single Spring Boot WebSocket server, which is the sole source of truth for the maze layout, player position, traps, and game timer. Neither client is trusted to report its own state — the server validates every move and broadcasts the authoritative result.

```
Navigator client  ──┐
 (sees full maze)    │
                      ├──►  Spring Boot WebSocket server
Explorer client  ──┘        (maze, position, timer, traps — in-memory)
 (sees only darkness)
```

- The **Navigator** never sends move commands — it only receives live position updates and trap data.
- The **Explorer** never receives the maze grid — only confirmed position updates after each move.
- The server generates the maze (recursive backtracker algorithm) so it can never be inspected via browser dev tools by either player.

## Project structure

```
invisible-maze-server/     Spring Boot backend
├── config/                 WebSocket registration
├── websocket/               MazeWebSocketHandler — routes incoming messages
├── model/                   GameRoom, Position, PlayerRole, GameStatus
├── service/                  RoomManager, MazeGenerator
└── message/                  DTOs for client/server communication

invisible-maze-client/      React + TypeScript + three.js frontend
├── pages/                    Lobby, NavigatorView, ExplorerView
├── three/                     Scene setup, split into shared / navigator / explorer
├── hooks/                     useWebSocket, useGameState
├── types/                     Shared message and game state types
└── components/                Timer, room code input, win/lose screens
```

## WebSocket message protocol

**Client → Server**
| Type | Payload | Sent by |
|---|---|---|
| `JOIN_ROOM` | `{ roomCode }` | Both |
| `MOVE` | `{ direction }` | Explorer |

**Server → Client**
| Type | Payload | Sent to |
|---|---|---|
| `MAZE_DATA` | Full grid, trap positions, exit location | Navigator only |
| `POSITION_UPDATE` | `{ x, y }` | Both |
| `TRAP_HIT` | — | Both |
| `GAME_WON` / `GAME_OVER` | — | Both |

## Getting started

### Backend
```bash
cd invisible-maze-server
./mvnw spring-boot:run
```
Runs on `localhost:8080`, WebSocket endpoint at `/game`.

### Frontend
Locate your local index.html file directory.
Open index.html inside a standard browser window (Chrome, Edge, Brave).
Open a separate Incognito Tab pointing to the same file to run as Player 2.

### Current status
🚧 In Active Development. The core real-time network sync loop is completely functional! The server-authoritative movement engine validates player steps against wall collisions, instantly updating the low-poly humanoid character model across both screens simultaneously on a 6x6 test grid.

### Roadmap
[x] Basic WebSocket connection handshake pipeline
[x] Room creation and join-by-code configuration
[x] Server-authoritative step validation (Wall collision checking)
[x] Immersive third-person camera perspective with Fog-of-War overlay for the Explorer
[x] Dynamic rotating overhead display configuration for the Navigator
[x] Low-poly Humanoid character avatar assembly (replaces temporary sphere markers)
[ ] Upgrade to high-density structural maze layouts (16x16 Matrix maps with 10-15 turns)
[ ] Navigator map selection preview menu
[ ] Round timer mechanics and trap modules
[ ] Win / Lose state checking loop
[ ] Polished frontend UI lobby redesign

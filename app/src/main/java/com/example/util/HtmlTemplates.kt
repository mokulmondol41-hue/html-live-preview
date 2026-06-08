package com.example.util

object HtmlTemplates {

    val PORTFOLIO = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>My Creative Space</title>
            <style>
                :root {
                    --bg-dark: #0f172a;
                    --card-bg: #1e293b;
                    --accent: #6366f1;
                    --text-main: #f1f5f9;
                    --text-muted: #94a3b8;
                }
                body {
                    margin: 0;
                    padding: 0;
                    background-color: var(--bg-dark);
                    color: var(--text-main);
                    font-family: system-ui, -apple-system, sans-serif;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    text-align: center;
                }
                .container {
                    max-width: 600px;
                    padding: 2.5rem;
                    background: var(--card-bg);
                    border-radius: 20px;
                    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    margin: 1.5rem;
                }
                .avatar {
                    width: 100px;
                    height: 100px;
                    background: linear-gradient(135deg, #6366f1, #a855f7);
                    border-radius: 50%;
                    margin: 0 auto 1.5rem;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 2.5rem;
                    font-weight: bold;
                    color: white;
                    box-shadow: 0 0 20px rgba(99, 102, 241, 0.5);
                }
                h1 {
                    font-size: 2rem;
                    margin: 0 0 0.5rem;
                    font-weight: 800;
                    background: linear-gradient(to right, #818cf8, #c084fc);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                }
                p.headline {
                    font-size: 1.1rem;
                    color: var(--text-muted);
                    margin-bottom: 2rem;
                }
                .links {
                    display: flex;
                    flex-direction: column;
                    gap: 1rem;
                }
                .link-card {
                    display: block;
                    padding: 1rem;
                    background: rgba(255, 255, 255, 0.05);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    border-radius: 12px;
                    color: var(--text-main);
                    text-decoration: none;
                    font-weight: 600;
                    transition: all 0.3s ease;
                }
                .link-card:hover {
                    background: var(--accent);
                    transform: translateY(-3px);
                    box-shadow: 0 5px 15px rgba(99, 102, 241, 0.4);
                    border-color: var(--accent);
                }
                .footer {
                    margin-top: 2.5rem;
                    font-size: 0.85rem;
                    color: var(--text-muted);
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="avatar">👨‍💻</div>
                <h1>Alex Developer</h1>
                <p class="headline">Product Designer & Full-Stack Engineer</p>
                <div class="links">
                    <a href="#" class="link-card" onclick="alert('Viewing Portfolio!')">💼 Explore Portfolio</a>
                    <a href="#" class="link-card" onclick="alert('Viewing GitHub!')">🛠️ Open GitHub Profile</a>
                    <a href="#" class="link-card" onclick="alert('Viewing Contact Info!')">✉️ Get In Touch</a>
                </div>
                <div class="footer">
                    Created via HTML Live Preview & Hosting Tool
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

    val INTERACTIVE_COUNTER = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Reactive Spark</title>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    background: radial-gradient(circle at center, #111827, #030712);
                    color: #fff;
                    font-family: system-ui, -apple-system, sans-serif;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                }
                .counter-box {
                    text-align: center;
                    background: rgba(255, 255, 255, 0.03);
                    border: 1px solid rgba(255, 255, 255, 0.08);
                    padding: 3rem;
                    border-radius: 24px;
                    backdrop-filter: blur(10px);
                    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6);
                }
                h2 {
                    font-size: 1.5rem;
                    color: #10b981;
                    margin-top: 0;
                    letter-spacing: 2px;
                    text-transform: uppercase;
                }
                #count-display {
                    font-size: 5rem;
                    font-weight: 800;
                    margin: 1.5rem 0;
                    background: linear-gradient(to right, #34d399, #059669);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    transition: transform 0.1s ease;
                }
                .btn-group {
                    display: flex;
                    gap: 1rem;
                    justify-content: center;
                }
                button {
                    background: #10b981;
                    color: #fff;
                    border: none;
                    padding: 1rem 1.8rem;
                    font-size: 1.2rem;
                    font-weight: bold;
                    border-radius: 12px;
                    cursor: pointer;
                    transition: all 0.2s ease;
                }
                button:hover {
                    background: #059669;
                    transform: scale(1.05);
                }
                button:active {
                    transform: scale(0.95);
                }
                button.secondary {
                    background: rgba(255, 255, 255, 0.1);
                    color: #fff;
                }
                button.secondary:hover {
                    background: rgba(255, 255, 255, 0.2);
                }
            </style>
        </head>
        <body>
            <div class="counter-box">
                <h2>Interactive Sandbox</h2>
                <div id="count-display">0</div>
                <div class="btn-group">
                    <button onclick="decrement()">-1</button>
                    <button class="secondary" onclick="reset()">Reset</button>
                    <button onclick="increment()">+1</button>
                </div>
            </div>

            <script>
                let count = 0;
                const display = document.getElementById('count-display');

                function updateDisplay() {
                    display.innerText = count;
                    display.style.transform = 'scale(1.2)';
                    setTimeout(() => {
                        display.style.transform = 'scale(1)';
                    }, 100);
                }

                function increment() {
                    count++;
                    updateDisplay();
                }

                function decrement() {
                    count--;
                    updateDisplay();
                }

                fun = function reset() {
                    count = 0;
                    updateDisplay();
                }
                // Fixing standard fallback if overwrite
                window.reset = function() {
                    count = 0;
                    updateDisplay();
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    val NEON_GRADIENT = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Glass Card</title>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    background: #000;
                    color: #fff;
                    font-family: system-ui, -apple-system, sans-serif;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    overflow: hidden;
                }
                .glow-orb {
                    position: absolute;
                    width: 300px;
                    height: 300px;
                    background: radial-gradient(circle, rgba(236, 72, 153, 0.4) 0%, rgba(0,0,0,0) 70%);
                    top: 20%;
                    left: 20%;
                    z-index: 1;
                    filter: blur(50px);
                    animation: float 8s infinite alternate;
                }
                .glow-orb-2 {
                    position: absolute;
                    width: 300px;
                    height: 300px;
                    background: radial-gradient(circle, rgba(59, 130, 246, 0.4) 0%, rgba(0,0,0,0) 70%);
                    bottom: 20%;
                    right: 20%;
                    z-index: 1;
                    filter: blur(50px);
                    animation: float 10s infinite alternate-reverse;
                }
                .glass-card {
                    position: relative;
                    z-index: 10;
                    background: rgba(255, 255, 255, 0.05);
                    backdrop-filter: blur(20px);
                    -webkit-backdrop-filter: blur(20px);
                    border: 1px solid rgba(255, 255, 255, 0.15);
                    border-radius: 20px;
                    padding: 2.5rem;
                    width: 320px;
                    box-shadow: 0 40px 100px rgba(0,0,0,0.8);
                    text-align: center;
                }
                h3 {
                    font-size: 1.6rem;
                    margin: 0 0 1rem;
                    background: linear-gradient(45deg, #ec4899, #3b82f6);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                }
                p {
                    font-size: 0.95rem;
                    line-height: 1.6;
                    color: #d1d5db;
                    margin-bottom: 2rem;
                }
                .pulse-btn {
                    display: inline-block;
                    background: linear-gradient(45deg, #ec4899, #3b82f6);
                    border: none;
                    color: white;
                    padding: 0.8rem 2rem;
                    border-radius: 50px;
                    font-weight: bold;
                    text-shadow: 0 1px 2px rgba(0,0,0,0.2);
                    box-shadow: 0 4px 15px rgba(236, 72, 153, 0.3);
                    cursor: pointer;
                    transition: transform 0.2s, box-shadow 0.2s;
                }
                .pulse-btn:hover {
                    transform: scale(1.03);
                    box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
                }
                @keyframes float {
                    0% { transform: translateY(0) scale(1); }
                    100% { transform: translateY(-40px) scale(1.2); }
                }
            </style>
        </head>
        <body>
            <div class="glow-orb"></div>
            <div class="glow-orb-2"></div>
            <div class="glass-card">
                <h3>Futuristic Glass</h3>
                <p>This UI elements renders in absolute base64 inside a secure high-contrast preview frame with custom glow filters, demonstrating hardware acceleration capabilities.</p>
                <button class="pulse-btn" onclick="alert('Glass Pulsed!')">Interact</button>
            </div>
        </body>
        </html>
    """.trimIndent()
}

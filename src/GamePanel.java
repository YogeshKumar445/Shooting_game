import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    public static final int WIDTH = 900, HEIGHT = 650;
    private Thread gameThread;
    private boolean running = false;
    GameState state = GameState.MENU;

    Player player;
    List<Bullet> bullets = Collections.synchronizedList(new ArrayList<>());
    List<Enemy> enemies = Collections.synchronizedList(new ArrayList<>());
    Random rng = new Random();

    int score = 0, level = 1;
    int enemySpawnTimer = 0, enemySpawnInterval = 50;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        initGame();
        startGame();
    }

    void initGame() {
        player = new Player(WIDTH/2-40, HEIGHT-120, 88);
        bullets.clear();
        enemies.clear();
        score=0; level=1; enemySpawnInterval=50; enemySpawnTimer=0;
    }

    void startGame() {
        if (gameThread == null || !running) {
            running=true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        int FPS=60;
        double TPF=1_000_000_000.0/FPS;
        long last = System.nanoTime();
        double delta = 0;
        while(running) {
            long now = System.nanoTime();
            delta += (now-last)/TPF;
            last=now;
            while(delta>=1) { tick(); delta--; }
            repaint();
            try { Thread.sleep(2); } catch(Exception e) {}
        }
    }

    void tick() {
        switch(state) {
            case MENU: break;
            case RUNNING:
                player.update(this);
                synchronized(bullets){ bullets.forEach(b->b.update(this)); }
                synchronized(enemies){ enemies.forEach(e->e.update(this)); }
                handleCollisions(); removeDead(); spawnLogic();
                break;
            case PAUSED: break;
            case GAME_OVER: break;
        }
    }

    void spawnLogic() {
        enemySpawnTimer++;
        if(enemySpawnTimer>=enemySpawnInterval){ spawnEnemy(); enemySpawnTimer=0; }
        if(score>level*20){ level++; enemySpawnInterval=Math.max(12, enemySpawnInterval-6); }
    }

    void spawnEnemy() {
        int size=50+rng.nextInt(40), x=rng.nextInt(WIDTH-size), y=-size, speed=2+rng.nextInt(2)+level/2;
        EnemyBehavior beh = rng.nextBoolean()? new StraightDownBehavior(): new ZigZagBehavior();
        enemies.add(new Enemy(x,y,size,speed,beh));
    }

    void handleCollisions() {
        synchronized(bullets){ synchronized(enemies){
            for(Iterator<Bullet> ib=bullets.iterator(); ib.hasNext();){
                Bullet b=ib.next();
                for(Iterator<Enemy> ie=enemies.iterator();ie.hasNext();){
                    Enemy e=ie.next();
                    if(b.getBounds().intersects(e.getBounds())){
                        b.alive=false; e.alive=false; score+=10; break;
                    }
                }
            }
        }}
        synchronized(enemies){
            for(Iterator<Enemy> ie=enemies.iterator();ie.hasNext();){
                Enemy e=ie.next();
                if(e.getBounds().intersects(player.getBounds())){
                    e.alive=false; player.lives--;
                    if(player.lives<=0) state=GameState.GAME_OVER;
                }
            }
        }
    }

    void removeDead(){ bullets.removeIf(b->!b.alive);
        enemies.removeIf(e->!e.alive||e.y>HEIGHT+100);
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch(state){
            case MENU: drawMenu(g2); break;
            case RUNNING: drawGame(g2); break;
            case PAUSED: drawPause(g2); break;
            case GAME_OVER: drawGameOver(g2); break;
        }
        g2.dispose();
    }

    void drawMenu(Graphics2D g){
        g.setColor(Color.WHITE);
        g.setFont(new Font("Verdana",Font.BOLD,48));
        String txt="Beat It - 2D-Shooting Game ";
        g.drawString(txt,WIDTH/2-g.getFontMetrics().stringWidth(txt)/2,HEIGHT/3);
        g.setFont(new Font("Arial",Font.PLAIN,18));
        g.drawString("ENTER to Start | Arrow keys to move | SPACE to shoot | P to Pause",
                WIDTH/2-g.getFontMetrics().stringWidth("ENTER to Start | Arrow keys to move | SPACE to shoot | P to Pause")/2,HEIGHT/2);
    }

    void drawGame(Graphics2D g){
        g.setColor(Color.BLACK); g.fillRect(0,0,WIDTH,HEIGHT);
        player.render(g);
        bullets.forEach(b->b.render(g));
        enemies.forEach(e->e.render(g));
        g.setColor(Color.WHITE); g.setFont(new Font("Arial",Font.BOLD,16));
        g.drawString("Score: "+score,12,20); g.drawString("Lives: "+player.lives,12,40);
        g.drawString("Level: "+level,WIDTH-120,20);
    }

    void drawPause(Graphics2D g){ drawGame(g); g.setColor(new Color(0,0,0,150));
        g.fillRect(0,0,WIDTH,HEIGHT);
        g.setColor(Color.YELLOW); g.setFont(new Font("Arial",Font.BOLD,48));
        g.drawString("PAUSED",WIDTH/2-100,HEIGHT/2);
    }

    void drawGameOver(Graphics2D g){ drawGame(g);
        g.setColor(new Color(0,0,0,200));
        g.fillRect(0,0,WIDTH,HEIGHT);
        g.setColor(Color.RED); g.setFont(new Font("Arial",Font.BOLD,48));
        g.drawString("GAME OVER",WIDTH/2-150,HEIGHT/2);
        g.setFont(new Font("Arial",Font.PLAIN,20));
        g.drawString("Press ENTER to Restart",WIDTH/2-110,HEIGHT/2+40);
    }

    void shoot(){
        long now=System.currentTimeMillis();
        if(now-player.lastShotTime<player.fireCooldown) return;
        player.lastShotTime=now;
        int bx=player.x+player.size/2-6, by=player.y-8;
        bullets.add(new Bullet(bx - 15, by, 20, -10));
        bullets.add(new Bullet(bx, by, 20, -12));
        bullets.add(new Bullet(bx + 15, by, 20, -10));

    }

    @Override
    public void keyPressed(KeyEvent e){
        int code=e.getKeyCode();
        if(state==GameState.MENU && code==KeyEvent.VK_ENTER){ initGame(); state=GameState.RUNNING; return; }
        if(state==GameState.GAME_OVER && code==KeyEvent.VK_ENTER){ initGame(); state=GameState.RUNNING; return; }
        if(code==KeyEvent.VK_P) { state=(state==GameState.RUNNING? GameState.PAUSED: GameState.RUNNING); return; }

        if(state==GameState.RUNNING){
            switch(code){
                case KeyEvent.VK_LEFT: player.left=true; break;
                case KeyEvent.VK_RIGHT: player.right=true; break;
                case KeyEvent.VK_UP: player.up=true; break;
                case KeyEvent.VK_DOWN: player.down=true; break;
                case KeyEvent.VK_SPACE: shoot(); break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e){
        switch(e.getKeyCode()){
            case KeyEvent.VK_LEFT: player.left=false; break;
            case KeyEvent.VK_RIGHT: player.right=false; break;
            case KeyEvent.VK_UP: player.up=false; break;
            case KeyEvent.VK_DOWN: player.down=false; break;
        }
    }
    @Override public void keyTyped(KeyEvent e){}
}

import java.awt.*;

enum GameState{
    MENU,RUNNING,PAUSED,GAME_OVER
}

interface EnemyBehavior{
    void move(Enemy e, GamePanel gp);
}

class StraightDownBehavior implements EnemyBehavior{
    public void move(Enemy e, GamePanel gp){ e.y+=e.speed;
    }
}

class ZigZagBehavior implements EnemyBehavior{
    private int dir=1;
    public void move(Enemy e, GamePanel gp){
        e.y+=e.speed;
        e.x+=dir*e.speed;
        if(e.x<0||e.x>GamePanel.WIDTH-e.size) dir*=-1;
    }
}

abstract class Entity{ int x,y,size; Rectangle getBounds(){
    return new Rectangle(x,y,size,size);
}
    abstract void update(GamePanel gp);
    abstract void render(Graphics2D g2);
}

class Player extends Entity{
    int speed=6,lives=3; boolean left,right,up,down;
    long lastShotTime=0, fireCooldown=180;
    Player(int x,int y,int size){
        this.x=x;
        this.y=y;
        this.size=size;
    }
    void update(GamePanel gp){
        if(left)x-=speed;
        if(right)x+=speed;
        if(up)y-=speed;
        if(down)y+=speed;
        x=Math.max(0,Math.min(GamePanel.WIDTH-size,x));
        y=Math.max(0,Math.min(GamePanel.HEIGHT-size,y));
    }
    void render(Graphics2D g2){
        g2.setColor(Color.BLUE);
        g2.fillOval(x,y,size,size);
    }
}

class Bullet extends Entity{
    int dy;
    boolean alive=true;
    Bullet(int x,int y,int size,int dy){
        this.x=x;
        this.y=y;
        this.size=size;
        this.dy=dy;}
    void update(GamePanel gp){
        y+=dy;
        if(y+size<0||y>GamePanel.HEIGHT) alive=false;}
    void render(Graphics2D g2){
        g2.setColor(Color.PINK);
        g2.fillRect(x,y,size/2,size);
    }
}

class Enemy extends Entity{
    int speed;
    EnemyBehavior behavior;
    boolean alive=true;
    Enemy(int x,int y,int size,int speed,EnemyBehavior b){
        this.x=x;
        this.y=y;
        this.size=size;
        this.speed=speed;
        this.behavior=b;}
    void update(GamePanel gp){
        behavior.move(this,gp);
        if(y>GamePanel.HEIGHT+200) alive=false;}
    void render(Graphics2D g2){
        g2.setColor(Color.RED);
        g2.fillRoundRect(x,y,size,size,6,6);
    }
}

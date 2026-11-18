package com.darkages.neondepths;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Neon Depths: a compact, systems-heavy dungeon-management game. */
public final class DungeonApp {
    private DungeonApp() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new GameWindow();
        });
    }

    enum Theme {
        SCI_FI("Sci-fi", new Color(20, 43, 58), new Color(64, 190, 216), new Color(18, 26, 40)),
        STEAMPUNK("Steampunk", new Color(67, 48, 34), new Color(211, 140, 62), new Color(31, 25, 23)),
        FANTASY("Fantasy", new Color(49, 55, 62), new Color(135, 186, 120), new Color(27, 30, 36)),
        DARK_AGES("Dark Ages", new Color(70, 64, 58), new Color(194, 169, 127), new Color(30, 27, 27));
        final String label; final Color floor, accent, voidColor;
        Theme(String label, Color floor, Color accent, Color voidColor) { this.label=label; this.floor=floor; this.accent=accent; this.voidColor=voidColor; }
        @Override public String toString() { return label; }
    }
    enum Difficulty {
        RELAXED("Relaxed", 1.45, .55), STANDARD("Standard", 1.0, 1.0), BRUTAL("Brutal", .72, 1.8), NIGHTMARE("Nightmare", .55, 2.7);
        final String label; final double startingResources, danger;
        Difficulty(String label, double resources, double danger) { this.label=label; startingResources=resources; this.danger=danger; }
        @Override public String toString() { return label; }
    }
    enum Mode {
        CAMPAIGN("Campaign"), ENDLESS("Endless"), SIEGE("Siege"), SANDBOX("Sandbox");
        final String label; Mode(String label) { this.label=label; }
        @Override public String toString() { return label; }
    }
    enum Tile {
        VOID("Erase", 0, ' ', false), EMPTY("Cavern", 0, '·', false), WALL("Wall", 8, '▦', true), FLOOR("Floor", 4, '▫', false), DOOR("Door", 12, '▥', false),
        CORE("Dungeon Heart", 0, '◆', true), TREASURY("Treasury", 18, '$', false), LAIR("Monster Lair", 22, '♟', false), FARM("Fungus Farm", 14, '♣', false),
        LIBRARY("Library", 20, 'Ω', false), WORKSHOP("Workshop", 22, '⚒', false), BARRACKS("Barracks", 18, '⚔', false), TEMPLE("Temple", 24, '✚', false),
        TRAP("Spike Trap", 16, '^', false), TURRET("Turret", 26, '⌖', true), GATE("Security Gate", 20, '╫', true), WATER("Water", 0, '~', true), LAVA("Lava", 0, '≈', true),
        PORTAL("Portal", 32, '◎', false), CRYSTAL("Crystal", 12, '♦', true), BRIDGE("Bridge", 8, '=', false);
        final String label; final int cost; final char glyph; final boolean blocks;
        Tile(String label,int cost,char glyph,boolean blocks){this.label=label;this.cost=cost;this.glyph=glyph;this.blocks=blocks;}
        @Override public String toString(){return label+(cost>0?" · "+cost:" ");}
    }
    static final class Unit {
        int x,y,hp; boolean guard; String name;
        Unit(int x,int y,int hp,boolean guard,String name){this.x=x;this.y=y;this.hp=hp;this.guard=guard;this.name=name;}
    }
    static final class GameModel {
        static final int W=42,H=30;
        final Tile[][] map=new Tile[H][W];
        final java.util.List<Unit> units=new ArrayList<>();
        final java.util.List<String> log=new ArrayList<>();
        Theme theme=Theme.SCI_FI; Difficulty difficulty=Difficulty.STANDARD; Mode mode=Mode.CAMPAIGN;
        int day=1, gold=400, stone=260, iron=120, mana=60, food=80, threat=0, fame=0, research=0;
        int morale=70, timerTicks=0, raidNumber=0, guards=3, monsters=2;
        boolean paused=true, lost=false, won=false;
        String objective="Survive 30 days and defend the Dungeon Heart.";
        final Random random=new Random();
        GameModel(){generate();}
        void generate(){
            for(int y=0;y<H;y++) for(int x=0;x<W;x++) map[y][x]=(x==0||y==0||x==W-1||y==H-1)?Tile.WALL:Tile.EMPTY;
            room(4,4,10,8); room(17,4,10,8); room(30,4,8,8); room(7,18,11,8); room(23,18,13,8);
            connect(13,8,17,8); connect(27,8,30,8); connect(10,12,10,18); connect(22,8,22,18); connect(18,22,23,22);
            map[8][22]=Tile.CORE; map[8][13]=Tile.DOOR; map[8][27]=Tile.DOOR; map[15][10]=Tile.DOOR; map[22][20]=Tile.DOOR;
            map[6][6]=Tile.TREASURY; map[6][8]=Tile.FARM; map[6][19]=Tile.LAIR; map[6][24]=Tile.LIBRARY; map[6][33]=Tile.WORKSHOP; map[22][10]=Tile.BARRACKS; map[22][28]=Tile.TEMPLE;
            units.clear(); units.add(new Unit(20,8,100,true,"Warden")); units.add(new Unit(19,9,80,true,"Guard")); units.add(new Unit(23,9,80,true,"Guard"));
            log.clear(); note("The dungeon awakens. Build rooms, gather resources, and protect the Heart.");
        }
        void room(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)map[yy][xx]=(xx==x||yy==y||xx==x+w-1||yy==y+h-1)?Tile.WALL:Tile.FLOOR;}
        void connect(int x1,int y1,int x2,int y2){int x=x1,y=y1;while(x!=x2){if(map[y][x]==Tile.WALL)map[y][x]=Tile.FLOOR;x+=Integer.compare(x2,x);}while(y!=y2){if(map[y][x]==Tile.WALL)map[y][x]=Tile.FLOOR;y+=Integer.compare(y2,y);}if(map[y][x]==Tile.WALL)map[y][x]=Tile.FLOOR;}
        void note(String s){log.add("Day "+day+" · "+s);while(log.size()>9)log.remove(0);}
        boolean place(int x,int y,Tile t){if(x<1||y<1||x>=W-1||y>=H-1||lost||won)return false;Tile old=map[y][x];if(old==Tile.CORE||old==t)return false;if(t!=Tile.VOID&&!afford(t))return false;
            if(t!=Tile.VOID){spend(t);map[y][x]=t;}else{map[y][x]=Tile.EMPTY;}
            return true;}
        boolean afford(Tile t){return gold>=t.cost && (t==Tile.WALL||t==Tile.FLOOR||t==Tile.DOOR||t==Tile.GATE||t==Tile.BRIDGE||stone>=material(t));}
        int material(Tile t){return switch(t){case TURRET,WORKSHOP,GATE->12;case TEMPLE,PORTAL,LIBRARY->8;case TRAP,BARRACKS,LAIR->5;default->0;};}
        void spend(Tile t){gold-=t.cost;stone-=Math.min(stone,material(t));}
        void advanceDay(){if(lost||won)return;day++;int income=3*count(Tile.TREASURY)+2*count(Tile.WORKSHOP)+2*count(Tile.FARM);gold+=18+income;stone+=4+count(Tile.WORKSHOP);iron+=2+count(Tile.WORKSHOP);mana+=2+count(Tile.LIBRARY)+count(Tile.TEMPLE);food+=4*count(Tile.FARM)-units.size();
            morale=Math.max(0,Math.min(100,morale+(count(Tile.TEMPLE)>0?2:-2)+(food>0?1:-5)));
            if(food<0){food=0;morale-=8;note("Food shortage! Morale is falling.");}
            if(count(Tile.LIBRARY)>0)research+=2+count(Tile.LIBRARY);
            int pressure=(int)Math.ceil(difficulty.danger*(mode==Mode.SIEGE?1.55:1.0));threat+=pressure;fame+=Math.max(1,monsters/2);
            note("Dawn: +"+income+" bonus gold. Threat is now "+threat+".");
            if(day%Math.max(2,7-(int)difficulty.danger)==0 || mode==Mode.SIEGE&&day%3==0) raid();
            if(mode==Mode.CAMPAIGN&&day>=30&& !lost){won=true;note("Campaign complete! Your dungeon has survived.");}
            if(mode==Mode.SIEGE&&day>=20&&!lost){won=true;note("Siege broken! The dungeon stands.");}
            if(morale<=0){lost=true;note("The minions have abandoned the dungeon.");}
        }
        int count(Tile t){int n=0;for(Tile[] row:map)for(Tile q:row)if(q==t)n++;return n;}
        void raid(){raidNumber++;int n=Math.max(1,Math.min(8,1+day/5+(int)(difficulty.danger*2)));note("Raid "+raidNumber+"! "+n+" invader(s) enter from the northern passage.");
            for(int i=0;i<n;i++)units.add(new Unit(2+random.nextInt(W-4),1,60+(int)(difficulty.danger*15),false,"Invader"));
            resolveRaid();}
        void resolveRaid(){
            java.util.List<Unit> invaders=new ArrayList<>();for(Unit u:units)if(!u.guard)invaders.add(u);
            for(Unit e:invaders){int steps=0;while(steps++<18&&e.hp>0){Unit defender=null;double best=2.2;for(Unit u:units)if(u.guard){double d=Math.abs(u.x-e.x)+Math.abs(u.y-e.y);if(d<best){best=d;defender=u;}}
                    if(defender!=null){e.hp-=25;defender.hp-=18;if(e.hp<=0){units.remove(e);gold+=12;fame+=2;break;}if(defender.hp<=0){units.remove(defender);guards=Math.max(0,guards-1);note("A defender has fallen!");}}
                    else {if(Math.abs(e.x-22)+Math.abs(e.y-8)<=1){int damage=15+(int)(difficulty.danger*8);map[8][22]=Tile.CORE;morale-=damage;note("The Dungeon Heart is under attack! Morale -"+damage+".");break;}
                        int nx=e.x,ny=e.y;if(e.x<22)nx++;else if(e.x>22)nx--;else if(e.y<8)ny++;else if(e.y>8)ny--;if(!map[ny][nx].blocks){e.x=nx;e.y=ny;}else if(map[ny][nx]==Tile.TRAP){e.hp-=50;map[ny][nx]=Tile.FLOOR;}else if(map[ny][nx]==Tile.TURRET){e.hp-=35;}else{if(random.nextBoolean())map[ny][nx]=Tile.EMPTY;}}
                }if(e.hp<=0)units.remove(e);}
            if(invaders.stream().noneMatch(u->units.contains(u)))note("Raid repelled. The dungeon is safe—for now.");
            if(morale<=0){lost=true;note("The dungeon has fallen.");}
        }
        void recruit(){if(gold<65||food<5){note("Recruiting needs 65 gold and 5 food.");return;}gold-=65;food-=5;guards++;units.add(new Unit(21,9,90,true,"Guard"));note("A new guard joins the dungeon.");}
        void hatch(){if(mana<20||food<4){note("Hatching needs 20 mana and 4 food.");return;}mana-=20;food-=4;monsters++;units.add(new Unit(18,8,80,true,"Minion"));note("A loyal minion hatches from the lair.");}
        void research(){if(research<10){note("Need 10 research points to unlock a technology.");return;}research-=10;gold+=35;note("Research complete: efficient construction (+35 gold). ");}
        void tick(){if(paused||lost||won)return;timerTicks++;if(timerTicks>=20){timerTicks=0;advanceDay();}}
        void save(){Path p=savePath();try(DataOutputStream o=new DataOutputStream(Files.newOutputStream(p))){o.writeInt(0x4E445031);o.writeInt(day);o.writeInt(gold);o.writeInt(stone);o.writeInt(iron);o.writeInt(mana);o.writeInt(food);o.writeInt(threat);o.writeInt(fame);o.writeInt(research);o.writeInt(morale);o.writeInt(guards);o.writeInt(monsters);o.writeInt(theme.ordinal());o.writeInt(difficulty.ordinal());o.writeInt(mode.ordinal());o.writeBoolean(paused);for(Tile[] row:map)for(Tile t:row)o.writeByte(t.ordinal());o.writeInt(units.size());for(Unit u:units){o.writeInt(u.x);o.writeInt(u.y);o.writeInt(u.hp);o.writeBoolean(u.guard);o.writeUTF(u.name);}o.writeInt(log.size());for(String s:log)o.writeUTF(s);}catch(IOException ex){throw new IllegalStateException(ex);}}
        void load(){Path p=savePath();if(!Files.exists(p))throw new IllegalStateException("No save file exists yet.");try(DataInputStream in=new DataInputStream(Files.newInputStream(p))){if(in.readInt()!=0x4E445031)throw new IOException("Unrecognized save file.");day=in.readInt();gold=in.readInt();stone=in.readInt();iron=in.readInt();mana=in.readInt();food=in.readInt();threat=in.readInt();fame=in.readInt();research=in.readInt();morale=in.readInt();guards=in.readInt();monsters=in.readInt();theme=Theme.values()[in.readInt()];difficulty=Difficulty.values()[in.readInt()];mode=Mode.values()[in.readInt()];paused=in.readBoolean();for(int y=0;y<H;y++)for(int x=0;x<W;x++)map[y][x]=Tile.values()[in.readUnsignedByte()];units.clear();int n=in.readInt();for(int i=0;i<n;i++)units.add(new Unit(in.readInt(),in.readInt(),in.readInt(),in.readBoolean(),in.readUTF()));log.clear();n=in.readInt();for(int i=0;i<n;i++)log.add(in.readUTF());lost=false;won=false;}catch(IOException|RuntimeException ex){throw new IllegalStateException("Could not load save: "+ex.getMessage(),ex);}}
        static Path savePath(){return Path.of(System.getProperty("user.home"),"neon-depths.save");}
    }

    static final class GameWindow {
        final GameModel game=new GameModel(); final JFrame frame=new JFrame("NEON DEPTHS | Dungeon Management"); final Board board=new Board(game); final JLabel status=new JLabel(); final JTextArea events=new JTextArea(); final JComboBox<Tile> tool=new JComboBox<>(Tile.values()); final JComboBox<Theme> theme=new JComboBox<>(Theme.values()); final JComboBox<Difficulty> difficulty=new JComboBox<>(Difficulty.values()); final JComboBox<Mode> mode=new JComboBox<>(Mode.values());
        GameWindow(){frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);frame.setMinimumSize(new Dimension(1150,780));frame.setLayout(new BorderLayout(8,8));frame.getContentPane().setBackground(new Color(17,20,29));frame.add(topBar(),BorderLayout.NORTH);frame.add(board,BorderLayout.CENTER);frame.add(sidebar(),BorderLayout.EAST);frame.add(bottomBar(),BorderLayout.SOUTH);frame.pack();frame.setSize(1380,900);frame.setLocationRelativeTo(null);frame.setVisible(true);new Timer(500,e->{game.tick();refresh();}).start();}
        JComponent topBar(){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));p.setBackground(new Color(27,32,45));p.add(label("NEON DEPTHS",18,new Color(98,220,233)));p.add(label("THE DUNGEON IS YOURS TO RULE",12,Color.LIGHT_GRAY));p.add(label("Theme",12,Color.WHITE));p.add(theme);p.add(label("Difficulty",12,Color.WHITE));p.add(difficulty);p.add(label("Mode",12,Color.WHITE));p.add(mode);theme.setSelectedItem(game.theme);difficulty.setSelectedItem(game.difficulty);mode.setSelectedItem(game.mode);theme.addActionListener(e->{game.theme=(Theme)theme.getSelectedItem();board.repaint();});difficulty.addActionListener(e->{game.difficulty=(Difficulty)difficulty.getSelectedItem();});mode.addActionListener(e->{game.mode=(Mode)mode.getSelectedItem();});return p;}
        JComponent sidebar(){JPanel p=new JPanel();p.setPreferredSize(new Dimension(295,0));p.setBackground(new Color(27,32,45));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));p.add(label("OVERSEER CONSOLE",15,new Color(98,220,233)));p.add(Box.createVerticalStrut(8));status.setForeground(Color.WHITE);status.setFont(new Font("Monospaced",Font.PLAIN,13));p.add(status);p.add(Box.createVerticalStrut(9));p.add(label("BUILD / ERASE",13,new Color(238,193,111)));tool.setMaximumSize(new Dimension(260,30));p.add(tool);p.add(Box.createVerticalStrut(7));p.add(button("Recruit guard · 65 gold",game::recruit));p.add(button("Hatch minion · 20 mana",game::hatch));p.add(button("Research technology · 10 RP",game::research));p.add(Box.createVerticalStrut(12));p.add(label("MISSION",13,new Color(238,193,111)));JLabel objective=label(game.objective,12,Color.WHITE);objective.setAlignmentX(LEFT_ALIGNMENT);objective.setMaximumSize(new Dimension(260,55));p.add(objective);p.add(Box.createVerticalStrut(10));p.add(label("EVENT LOG",13,new Color(238,193,111)));events.setEditable(false);events.setLineWrap(true);events.setWrapStyleWord(true);events.setBackground(new Color(17,20,29));events.setForeground(new Color(210,218,230));events.setFont(new Font("Monospaced",Font.PLAIN,11));JScrollPane scroll=new JScrollPane(events);scroll.setPreferredSize(new Dimension(265,300));scroll.setMaximumSize(new Dimension(265,300));p.add(scroll);return p;}
        JComponent bottomBar(){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));p.setBackground(new Color(27,32,45));p.add(button("Advance day",()->game.advanceDay()));p.add(button("Start / pause",()->game.paused=!game.paused));p.add(button("Save",()->runAction(game::save,"Dungeon saved.")));p.add(button("Load",()->runAction(game::load,"Dungeon loaded.")));p.add(button("New dungeon",()->{if(JOptionPane.showConfirmDialog(frame,"Start a new dungeon? Unsaved progress will be lost.","New dungeon",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){game.generate();game.day=1;game.gold=400;game.stone=260;game.iron=120;game.mana=60;game.food=80;game.threat=0;game.fame=0;game.research=0;game.morale=70;game.lost=false;game.won=false;}}));p.add(label("Click / drag to build · Right-click erases · Space pauses · Save: ~/neon-depths.save",11,new Color(190,197,210)));return p;}
        JButton button(String text,Runnable action){JButton b=new JButton(text);b.setAlignmentX(LEFT_ALIGNMENT);b.setMaximumSize(new Dimension(270,32));b.addActionListener(e->{action.run();refresh();});return b;}
        JLabel label(String text,int size,Color color){JLabel l=new JLabel(text);l.setFont(new Font("Dialog",Font.BOLD,size));l.setForeground(color);l.setAlignmentX(LEFT_ALIGNMENT);return l;}
        void runAction(Runnable a,String success){try{a.run();}catch(Exception ex){JOptionPane.showMessageDialog(frame,ex.getMessage(),"Notice",JOptionPane.WARNING_MESSAGE);return;}JOptionPane.showMessageDialog(frame,success);}
        void refresh(){status.setText("DAY       "+game.day+"   "+(game.paused?"[PAUSED]":"[LIVE]")+"\nGOLD      "+game.gold+"\nSTONE     "+game.stone+"\nIRON      "+game.iron+"\nMANA      "+game.mana+"\nFOOD      "+game.food+"\nMORALE    "+game.morale+" / 100\nTHREAT    "+game.threat+"\nFAME      "+game.fame+"\nRESEARCH  "+game.research+" RP\nGUARDS    "+game.guards+"   MINIONS "+game.monsters);events.setText(String.join("\n",game.log));events.setCaretPosition(events.getDocument().getLength());if(game.won||game.lost){JOptionPane.showMessageDialog(frame,game.won?"Victory! Your dungeon survived the campaign.":"Defeat. The dungeon's morale has collapsed.");game.paused=true;game.won=false;}board.repaint();}
    }

    static final class Board extends JPanel {
        final GameModel game; final int size=20; Tile current=Tile.FLOOR; boolean mouseDown=false;
        Board(GameModel game){this.game=game;setPreferredSize(new Dimension(GameModel.W*size,GameModel.H*size));setBackground(Color.BLACK);setFocusable(true);addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){mouseDown=true;edit(e);}public void mouseReleased(MouseEvent e){mouseDown=false;}});addMouseMotionListener(new MouseMotionAdapter(){public void mouseDragged(MouseEvent e){if(mouseDown)edit(e);}});addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_SPACE)game.paused=!game.paused;}});}
        void edit(MouseEvent e){if(!isShowing())return;Container c=getParent();while(c!=null&&!(c instanceof GameWindow))c=c.getParent();GameWindow w=(GameWindow)c;Tile t=(Tile)w.tool.getSelectedItem();if(t==null)return;int x=e.getX()/size,y=e.getY()/size;boolean changed=game.place(x,y,e.getButton()==MouseEvent.BUTTON3?Tile.VOID:t);if(changed)repaint();else if(e.getButton()==MouseEvent.BUTTON1&&t.cost>0&&x>0&&y>0&&x<GameModel.W-1&&y<GameModel.H-1){Toolkit.getDefaultToolkit().beep();}}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);for(int y=0;y<GameModel.H;y++)for(int x=0;x<GameModel.W;x++)drawTile(g,x,y,game.map[y][x]);for(Unit u:game.units){int px=u.x*size,py=u.y*size;g.setColor(u.guard?new Color(96,221,156):new Color(239,82,82));g.fill(new Ellipse2D.Double(px+4,py+4,12,12));g.setColor(Color.WHITE);g.setFont(new Font("Dialog",Font.BOLD,9));g.drawString(u.guard?"+":"!",px+8,py+13);}g.setColor(new Color(255,255,255,70));g.drawRect(0,0,GameModel.W*size-1,GameModel.H*size-1);g.dispose();}
        void drawTile(Graphics2D g,int x,int y,Tile t){int px=x*size,py=y*size;Color base=game.theme.floor;switch(t){case EMPTY->base=game.theme.voidColor;case WALL,GATE->base=game.theme.accent.darker();case WATER->base=new Color(34,103,158);case LAVA->base=new Color(181,70,37);case CORE->base=new Color(148,68,195);case TRAP->base=new Color(119,87,52);case PORTAL->base=new Color(87,61,165);case CRYSTAL->base=new Color(67,115,130);default->{}}g.setColor(base);g.fillRect(px,py,size,size);g.setColor(new Color(255,255,255,17));g.drawRect(px,py,size-1,size-1);if(t!=Tile.EMPTY&&t!=Tile.FLOOR&&t!=Tile.WALL){g.setColor(t==Tile.CORE?Color.WHITE:game.theme.accent.brighter());g.setFont(new Font("Dialog",Font.BOLD,14));String s=String.valueOf(t.glyph);FontMetrics fm=g.getFontMetrics();g.drawString(s,px+(size-fm.stringWidth(s))/2,py+15);}else if(t==Tile.WALL){g.setColor(new Color(0,0,0,80));g.drawLine(px+3,py+6,px+17,py+6);g.drawLine(px+3,py+14,px+17,py+14);}else if(t==Tile.FLOOR){g.setColor(new Color(255,255,255,28));g.drawLine(px+4,py+10,px+16,py+10);}}
    }
}

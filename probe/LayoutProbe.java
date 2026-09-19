package probe;

import com.gameverse.player.Player;
import com.gameverse.ui.MainMenu;

import javax.swing.*;
import java.awt.*;

/** Prints game-card positions across window sizes to verify grid alignment. */
public class LayoutProbe {

    public static void main(String[] args) throws Exception {
        MainMenu[] holder = new MainMenu[1];
        SwingUtilities.invokeAndWait(() -> {
            Player p = new Player("probe@gameverse.dev");
            MainMenu menu = new MainMenu(p, new MainMenu.MainMenuCallback() {
                @Override public void onPlayGame(String gameName) { }
                @Override public void onLogout() { }
            });
            menu.setExtendedState(Frame.NORMAL);
            holder[0] = menu;
        });

        MainMenu menu = holder[0];
        int[][] sizes = {{1280, 800}, {1600, 900}, {1920, 1080}};
        for (int[] s : sizes) {
            final int w = s[0], h = s[1];
            SwingUtilities.invokeAndWait(() -> {
                menu.setSize(w, h);
                menu.validate();
            });
            Thread.sleep(300);
            final int[][] span = new int[1][];
            SwingUtilities.invokeAndWait(() -> span[0] = cardSpan(menu.getContentPane()));
            int[] sp = span[0];
            System.out.printf("WINDOW %dx%d: cards x=[%d..%d] viewportW=%d  -> right gap=%d, left gap=%d%n",
                w, h, sp[0], sp[1], menu.getContentPane().getWidth(),
                sp[1] - menu.getContentPane().getWidth(), sp[0]);
        }
        SwingUtilities.invokeLater(() -> System.exit(0));
        Thread.sleep(400);
    }

    /** Returns [minX, maxX] of all GameCards in frame coordinates. */
    private static int[] cardSpan(Container root) {
        final int[] span = {Integer.MAX_VALUE, 0};
        walk(root, span);
        if (span[0] == Integer.MAX_VALUE) span[0] = 0;
        return span;
    }

    private static void walk(Component c, int[] span) {
        if (c.getClass().getSimpleName().equals("GameCard")) {
            Window win = null;
            Component k = c;
            while (k != null) { if (k instanceof Window w) { win = (Window) k; break; } k = k.getParent(); }
            Point p = SwingUtilities.convertPoint(c.getParent(), c.getLocation(), win);
            span[0] = Math.min(span[0], p.x);
            span[1] = Math.max(span[1], p.x + c.getWidth());
        }
        if (c instanceof Container cont) {
            for (Component k : cont.getComponents()) walk(k, span);
        }
    }
}

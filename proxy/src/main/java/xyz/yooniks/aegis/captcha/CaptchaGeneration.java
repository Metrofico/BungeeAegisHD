package xyz.yooniks.aegis.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.caching.CachedCaptcha;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.captcha.generator.CaptchaPainter;
import xyz.yooniks.aegis.captcha.generator.map.CraftMapCanvas;
import xyz.yooniks.aegis.captcha.generator.map.MapPalette;
import xyz.yooniks.aegis.packets.MapDataPacket;

/**
 * @author Leymooo
 */
public class CaptchaGeneration
{

  private static int enablingPercent;

  public static int getEnablingPercent() {
    return enablingPercent;
  }

  private final Random rnd = new Random();

  public void generateImages()
  {
    Font[] fonts = new Font[]
        {
            new Font( Font.SANS_SERIF, Font.PLAIN, 50 ),
            new Font( Font.SERIF, Font.PLAIN, 50 ),
            new Font( Font.MONOSPACED, Font.BOLD, 50 )
        };

    ExecutorService executor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
    CaptchaPainter painter = new CaptchaPainter();
    MapPalette.prepareColors();
    for ( int i = 100; i <= 999; i++ )
    {
      executor.execute( () ->
      {
        String answer = randomAnswer();
        BufferedImage image = painter.draw( fonts[rnd.nextInt( fonts.length )], randomNotWhiteColor(), answer );
        final CraftMapCanvas map = new CraftMapCanvas();
        map.drawImage( 0, 0, image );
        MapDataPacket packet = new MapDataPacket( 0, (byte) 0, map.getMapData() );
        PacketUtils.captchas.createCaptchaPacket( packet, answer );
      } );
    }

    long start = System.currentTimeMillis();
    ThreadPoolExecutor ex = (ThreadPoolExecutor) executor;
    while ( ex.getActiveCount() != 0 )
    {
      enablingPercent = 900 - ex.getQueue().size() - ex.getActiveCount();
      //BungeeCord.getInstance().getLogger().log( Level.INFO, "[BotFilter] Генерирую капчу [{0}/900]", 900 - ex.getQueue().size() - ex.getActiveCount() );
      try
      {
        Thread.sleep( 1000L );
      } catch ( InterruptedException ex1 )
      {
        BungeeCord.getInstance().getLogger().log( Level.WARNING, "[Aegis] Could not generate captcha packet!", ex1 );
        System.exit( 0 );
        return;
      }
    }
    CachedCaptcha.generated = true;
    executor.shutdownNow();
    System.gc();
    BungeeCord.getInstance().getLogger()
        .log(Level.INFO, "[Aegis] Generated captcha packet in {0}ms",
            System.currentTimeMillis() - start);  }


  private Color randomNotWhiteColor()
  {
    Color color = MapPalette.colors[rnd.nextInt( MapPalette.colors.length )];
    if ( color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255 )
    {
      return randomNotWhiteColor();
    }
    if ( color.getRed() == 220 && color.getGreen() == 220 && color.getBlue() == 220 )
    {
      return randomNotWhiteColor();
    }
    return color;
  }

  private String randomAnswer()
  {
    if ( rnd.nextBoolean() )
    {
      return Integer.toString( rnd.nextInt( ( 99999 - 10000 ) + 1 ) + 10000 );
    } else
    {
      return Integer.toString( rnd.nextInt( ( 9999 - 1000 ) + 1 ) + 1000 );
    }
  }
}

/*package xyz.yooniks.aegis.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.caching.CachedCaptcha;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.captcha.generator.CaptchaPainter;
import xyz.yooniks.aegis.captcha.generator.map.CraftMapCanvas;
import xyz.yooniks.aegis.captcha.generator.map.MapPalette;
import xyz.yooniks.aegis.packets.MapDataPacket;

public class CaptchaGeneration {

  private static int enablingPercent;

  public void generateImages()
  {
    final Random rnd = new Random();
    final ThreadLocal<Font[]> fonts = ThreadLocal.withInitial( () -> new Font[]
        {
            new Font( Font.SANS_SERIF, Font.PLAIN, rnd.nextInt( 5 ) + 62 ),
            new Font( Font.SERIF, Font.PLAIN, rnd.nextInt( 5 ) + 62 ),
            new Font( Font.MONOSPACED, Font.BOLD, rnd.nextInt( 5 ) + 62 )
        } );

    ExecutorService executor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
    CaptchaPainter painter = new CaptchaPainter();
    MapPalette.prepareColors();
    for ( int i = 100; i <= 999; i++ )
    //for (int i = 12000; i <= 12999; i ++)
    {
      final int answer = i;
      executor.execute( () ->
      {
        Font[] curr = fonts.get();
        BufferedImage image = painter.draw( curr[rnd.nextInt( curr.length )], randomNotWhiteColor( rnd ),
            String.valueOf( answer ) );
        final CraftMapCanvas map = new CraftMapCanvas();
        map.drawImage( 0, 0, image );
        MapDataPacket packet = new MapDataPacket( 0, (byte) 0, map.getMapData() );
        PacketUtils.captchas.createCaptchaPacket( packet, answer );
      } );
    }

    long start = System.currentTimeMillis();
    ThreadPoolExecutor ex = (ThreadPoolExecutor) executor;
    while ( ex.getActiveCount() != 0 )
    {
      enablingPercent = 900 - ex.getQueue().size() - ex.getActiveCount();

      try
      {
        Thread.sleep( 1000L );
      } catch ( InterruptedException ex1 )
      {
        BungeeCord.getInstance().getLogger().log( Level.WARNING, "[Aegis] Could not generate captcha packet!", ex1 );
        System.exit( 0 );
        return;
      }
    }
    CachedCaptcha.generated = true;
    executor.shutdownNow();
    System.gc();
    BungeeCord.getInstance().getLogger()
        .log(Level.INFO, "[Aegis] Generated captcha packet in {0}ms",
            System.currentTimeMillis() - start);
  }


  private static Color randomNotWhiteColor(Random random)
  {
    Color color = MapPalette.colors[random.nextInt( MapPalette.colors.length )];
    if ( color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255 )
    {
      return randomNotWhiteColor( random );
    }
    return color;
  }

  public static int getEnablingPercent() {
    return enablingPercent;
  }

  /*private static int enablingPercent = -1;
  private Font[] fonts = new Font[]
      {
          new Font(Font.SANS_SERIF, Font.PLAIN, 128 / 2),
          new Font(Font.SERIF, Font.PLAIN, 128 / 2),
          new Font(Font.MONOSPACED, Font.BOLD, 128 / 2)
      };
  private CaptchaPainter painter = new CaptchaPainter();

  public CaptchaGeneration() {
    new Thread(() -> {
      for (int i = 100; i <= 999; i++) {
        final int answer = i;
          ThreadLocalRandom rnd = ThreadLocalRandom.current();
          BufferedImage image = painter.draw(fonts[rnd.nextInt(fonts.length)],
              MapPalette.colors[rnd.nextInt(MapPalette.colors.length)],
              String.valueOf(answer));
          final CraftMapCanvas map = new CraftMapCanvas();
          map.drawImage(0, 0, image);
          MapDataPacket packet = new MapDataPacket(0, (byte) 0, map.getMapData());
          PacketUtils.captchas.createCaptchaPacket(packet, answer);
        enablingPercent = i;
      }

      long start = System.currentTimeMillis();
      CachedCaptcha.generated = true;
      System.gc();
      BungeeCord.getInstance().getLogger()
          .log(Level.INFO, "[Aegis] Generated captcha packet in {0}ms",
              System.currentTimeMillis() - start);
      Thread.currentThread().interrupt();
    }).start();
  }

  public static int getEnablingPercent() {
    return enablingPercent;
  }*/
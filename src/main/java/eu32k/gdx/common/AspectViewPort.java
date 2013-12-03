package eu32k.gdx.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;

public class AspectViewPort extends Rectangle {
   private static final long serialVersionUID = 4673143930375593767L;

   private float minRatio;
   private float maxRatio;

   public AspectViewPort(float minWidth, float maxWidth, float minHeight, float maxHeight) {
      super(0.0f, 0.0f, minWidth, minHeight);
      minRatio = minWidth / minHeight;
      maxRatio = maxWidth / maxHeight;
   }

   public AspectViewPort(float minRatio, float maxRatio) {
      super(0.0f, 0.0f, 1.0f, 1.0f);
      this.minRatio = minRatio;
      this.maxRatio = maxRatio;
   }

   public void update() {
      float w = Gdx.graphics.getWidth();
      float h = Gdx.graphics.getHeight();
      float ratio = w / h;

      System.out.println(w + " " + h + " " + ratio);

      if (ratio < minRatio) { // too high
         float newH = w / minRatio;
         set(0.0f, (h - newH) / 2.0f, w, newH);
      } else if (ratio > maxRatio) { // too wide
         float newW = h * maxRatio;
         set((w - newW) / 2.0f, 0, newW, h);
      } else { // ratio OK
         set(0.0f, 0.0f, w, h);
      }
   }
}

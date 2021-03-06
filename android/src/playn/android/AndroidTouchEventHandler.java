/**
 * Copyright 2011 The PlayN Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package playn.android;

import android.view.MotionEvent;

import pythagoras.f.IPoint;

import playn.core.Pointer;
import playn.core.Touch;

/**
 * Class for taking MotionEvents from GameActivity.onMotionEvent() and parsing
 * them into an array of Touch.Events for the Listener.
 */
class AndroidTouchEventHandler {

  private final AndroidGraphics graphics;
  private final GameViewGL gameView;

  AndroidTouchEventHandler(AndroidGraphics graphics, GameViewGL gameView) {
    this.graphics = graphics;
    this.gameView = gameView;
  }

  /**
   * Special implementation of Touch.Event.Impl for keeping track of changes to preventDefault
   */
  static class AndroidTouchEventImpl extends Touch.Event.Impl {
    final boolean[] preventDefault;

    public AndroidTouchEventImpl(double time, float x, float y, int id, boolean[] preventDefault) {
      super(time, x, y, id);
      this.preventDefault = preventDefault;
    }

    public AndroidTouchEventImpl(double time, float x, float y, int id, float pressure, float size,
        boolean[] preventDefault) {
      super(time, x, y, id, pressure, size);
      this.preventDefault = preventDefault;
    }

    @Override
    public void setPreventDefault(boolean preventDefault) {
      this.preventDefault[0] = preventDefault;
    }

    @Override
    public boolean getPreventDefault() {
      return preventDefault[0];
    }
  }

  /**
   * Default Android touch behavior. Parses the immediate MotionEvent and passes
   * it to the correct methods in {@GameViewGL} for processing
   * on the GL render thread. Ignores historical values.
   */
  public boolean onMotionEvent(MotionEvent nativeEvent) {
    double time = nativeEvent.getEventTime();
    int action = nativeEvent.getAction();
    boolean[] preventDefault = {false};

    Touch.Event.Impl[] touches = parseMotionEvent(nativeEvent, preventDefault);
    Touch.Event pointerEvent = touches[0];
    Pointer.Event.Impl event;

    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN:
        gameView.onTouchStart(touches);
        event = new Pointer.Event.Impl(time, pointerEvent.x(), pointerEvent.y(), true);
        gameView.onPointerStart(event);
        return (preventDefault[0] || event.getPreventDefault());
      case MotionEvent.ACTION_UP:
        gameView.onTouchEnd(touches);
        event = new Pointer.Event.Impl(time, pointerEvent.x(), pointerEvent.y(), true);
        gameView.onPointerEnd(event);
        return (preventDefault[0] || event.getPreventDefault());
      case MotionEvent.ACTION_POINTER_DOWN:
        gameView.onTouchStart(getChangedTouches(action, touches));
        return preventDefault[0];
      case MotionEvent.ACTION_POINTER_UP:
        gameView.onTouchEnd(getChangedTouches(action, touches));
        return preventDefault[0];
      case MotionEvent.ACTION_MOVE:
        gameView.onTouchMove(touches);
        event = new Pointer.Event.Impl(time, pointerEvent.x(), pointerEvent.y(), true);
        gameView.onPointerDrag(event);
        return (preventDefault[0] || event.getPreventDefault());
      case MotionEvent.ACTION_CANCEL:
        break;
      case MotionEvent.ACTION_OUTSIDE:
        break;
    }
    return false;
  }

  private Touch.Event.Impl[] getChangedTouches(int action, Touch.Event.Impl[] touches) {
    int changed = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
      >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    return new Touch.Event.Impl[] { touches[changed] };
  }

  /**
   * Performs the actual parsing of the MotionEvent event.
   *
   * @param event The MotionEvent to process
   * @param preventDefault Shared preventDefault state among returned {@link AndroidTouchEventImpl}
   * @return Processed array of {@link AndroidTouchEventImpl}s which share a preventDefault state.
   */
  private Touch.Event.Impl[] parseMotionEvent(MotionEvent event, boolean[] preventDefault) {
    int eventPointerCount = event.getPointerCount();
    Touch.Event.Impl[] touches = new Touch.Event.Impl[eventPointerCount];
    double time = event.getEventTime();
    float pressure, size;
    int id;
    for (int t = 0; t < eventPointerCount; t++) {
      int pointerIndex = t;
      IPoint xy = graphics.transformTouch(event.getX(pointerIndex), event.getY(pointerIndex));
      pressure = event.getPressure(pointerIndex);
      size = event.getSize(pointerIndex);
      id = event.getPointerId(pointerIndex);
      touches[t] = new AndroidTouchEventImpl(
        time, xy.x(), xy.y(), id, pressure, size, preventDefault);
    }
    return touches;
  }
}

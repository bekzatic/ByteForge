package com.byteforge.state;

import com.byteforge.observer.Order;
import com.byteforge.observer.OrderStatus;

/**
 * STATE pattern - the <b>State</b> interface.
 * <p>
 * What an order may do depends on where it is in its life cycle: a paid order can
 * still be cancelled, an order on the workbench cannot, a delivered order cannot move
 * any further. Instead of a growing {@code switch (status)} inside {@link Order},
 * each step is a class that knows its own rules and which step comes next.
 */
public interface OrderState {
    OrderStatus getStatus();

    /** Moves the order to the next step, or throws if this step is final. */
    void advance(Order order);

    /** Cancels the order, or throws if this step does not allow it. */
    void cancel(Order order, String reason);

    boolean canCancel();

    /** Text for the GUI explaining what can happen next. */
    String hint();
}

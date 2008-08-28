/*
 * Copyright (c) 2002-2008 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit.javascript.host;

import java.util.Arrays;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;

/**
 * JavaScript object representing an event that is passed into event handlers when they are
 * invoked. For general information on which properties and functions should be supported,
 * see <a href="http://developer.mozilla.org/en/docs/DOM:event">the mozilla docs</a>,
 * <a href="http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-Event">the W3C DOM
 * Level 2 Event Documentation</a> or <a href="http://msdn2.microsoft.com/en-us/library/aa703876.aspx">IE's
 * IHTMLEventObj interface</a>.
 *
 * @version $Revision$
 * @author <a href="mailto:chriseldredge@comcast.net">Chris Eldredge</a>
 * @author Mike Bowler
 * @author Chris Erskine
 * @author Marc Guillemot
 * @author Daniel Gredler
 * @author Brad Murray
 * @author Ahmed Ashour
 * @author Rob Di Marco
 */
public class Event extends SimpleScriptable {

    /**
     * Key to place the event's target in the Context's scope during event processing
     * to compute node coordinates compatible with those of the event.
     */
    static final String KEY_CURRENT_EVENT = "Event#current";

    /** The submit event type, triggered by "onsubmit" event handlers. */
    public static final String TYPE_SUBMIT = "submit";

    /** The change event type, triggered by "onchange" event handlers. */
    public static final String TYPE_CHANGE = "change";

    /** The load event type, triggered by "onload" event handlers. */
    public static final String TYPE_LOAD = "load";

    /** The unload event type, triggered by "onunload" event handlers. */
    public static final String TYPE_UNLOAD = "unload";

    /** The focus event type, triggered by "onfocus" event handlers. */
    public static final String TYPE_FOCUS = "focus";

    /** The focus in event type, triggered by "onfocusin" event handlers. */
    public static final String TYPE_FOCUS_IN = "focusin";

    /** The focus out event type, triggered by "onfocusout" event handlers. */
    public static final String TYPE_FOCUS_OUT = "focusout";

    /** The blur event type, triggered by "onblur" event handlers. */
    public static final String TYPE_BLUR = "blur";

    /** The key down event type, triggered by "onkeydown" event handlers. */
    public static final String TYPE_KEY_DOWN = "keydown";

    /** The key down event type, triggered by "onkeypress" event handlers. */
    public static final String TYPE_KEY_PRESS = "keypress";

    /** The key down event type, triggered by "onkeyup" event handlers. */
    public static final String TYPE_KEY_UP = "keyup";

    /** The submit event type, triggered by "onreset" event handlers. */
    public static final String TYPE_RESET = "reset";

    /** The beforeunload event type, triggered by "onbeforeunload" event handlers. */
    public static final String TYPE_BEFORE_UNLOAD = "beforeunload";

    /** Triggered after the DOM has loaded but before images etc. */
    public static final String TYPE_DOM_DOCUMENT_LOADED = "DOMContentLoaded";

    /** The property change event type, triggered by "onpropertychange" event handlers. */
    public static final String TYPE_PROPERTY_CHANGE = "propertychange";

    /** The first event phase: the capturing phase. */
    public static final short CAPTURING_PHASE = 1;

    /** The second event phase: at the event target. */
    public static final short AT_TARGET = 2;

    /** The third (and final) event phase: the bubbling phase. */
    public static final short BUBBLING_PHASE = 3;

    private static final long serialVersionUID = 4050485607908455730L;

    private Object srcElement_;        // IE-only writeable equivalent of target.
    private Object target_;            // W3C standard read-only equivalent of srcElement.
    private Object currentTarget_;     // Changes during event capturing and bubbling.
    private String type_;              // The event type.
    private Object keyCode_;           // Key code for a keypress
    private boolean shiftKey_;         // Exposed here in IE, only in mouse events in FF.
    private boolean ctrlKey_;          // Exposed here in IE, only in mouse events in FF.
    private boolean altKey_;           // Exposed here in IE, only in mouse events in FF.
    private String propertyName_;
    private boolean stopPropagation_;
    private Object returnValue_;
    private boolean preventDefault_;

    /**
     * The current event phase. This is a W3C standard attribute not implemented by IE. One of
     * {@link #CAPTURING_PHASE}, {@link #AT_TARGET} or {@link #BUBBLING_PHASE}.
     */
    private short eventPhase_;

    /**
     * Whether or not the event bubbles. The value of this attribute depends on the event type. To
     * determine if a certain event type bubbles, see http://www.w3.org/TR/DOM-Level-2-Events/events.html
     * Most event types do bubble, so this is true by default; event types which do not bubble should
     * overwrite this value in their constructors.
     */
    private boolean bubbles_ = true;

    /**
     * Whether or not the event can be canceled. The value of this attribute depends on the event type. To
     * determine if a certain event type can be canceled, see http://www.w3.org/TR/DOM-Level-2-Events/events.html
     * The more common event types are cancelable, so this is true by default; event types which cannot be
     * canceled should overwrite this value in their constructors.
     */
    private boolean cancelable_ = true;

    /**
     * The time at which the event was created.
     */
    private long timeStamp_ = System.currentTimeMillis();

    /**
     * Creates a new event instance.
     * @param domNode the DOM node that triggered the event
     * @param type the event type
     */
    public Event(final DomNode domNode, final String type) {
        this(domNode, type, false, false, false);
    }

    /**
     * Creates a new event instance.
     * @param domNode the DOM node that triggered the event
     * @param type the event type
     * @param shiftKey true if SHIFT is pressed
     * @param ctrlKey true if CTRL is pressed
     * @param altKey true if ALT is pressed
     */
    public Event(final DomNode domNode, final String type,
            final boolean shiftKey, final boolean ctrlKey, final boolean altKey) {
        final Object target = domNode.getScriptObject();
        srcElement_ = target;
        target_ = target;
        currentTarget_ = target;
        type_ = type;
        shiftKey_ = shiftKey;
        ctrlKey_ = ctrlKey;
        altKey_ = altKey;
        keyCode_ = Context.getUndefinedValue();
        setParentScope((SimpleScriptable) target);
        setPrototype(getPrototype(getClass()));
        setDomNode(domNode, false);
    }

    /**
     * Creates a new event instance for a keypress event.
     * @param domNode the DOM node that triggered the event
     * @param type the event type
     * @param keyCode the key code associated with the event
     * @param shiftKey true if SHIFT is pressed
     * @param ctrlKey true if CTRL is pressed
     * @param altKey true if ALT is pressed
     */
    public Event(final DomNode domNode, final String type, final int keyCode,
            final boolean shiftKey, final boolean ctrlKey, final boolean altKey) {
        this(domNode, type, shiftKey, ctrlKey, altKey);
        keyCode_ = new Integer(keyCode);
    }

    /**
     * Creates a new Event with {@link #TYPE_PROPERTY_CHANGE} type.
     * @param domNode the DOM node that triggered the event
     * @param propertyName the property name that was changed
     * @return the new Event object
     */
    public static Event createPropertyChangeEvent(final DomNode domNode, final String propertyName) {
        final Event event = new Event(domNode, TYPE_PROPERTY_CHANGE);
        event.propertyName_ = propertyName;
        return event;
    }

    /**
     * Used to build the prototype.
     */
    public Event() {
        // Empty.
    }

    /**
     * Called when the event starts being fired
     */
    void startFire() {
        Context.getCurrentContext().putThreadLocal(KEY_CURRENT_EVENT, this);
    }

    /**
     * Called when the event starts being fired
     */
    void endFire() {
        Context.getCurrentContext().removeThreadLocal(KEY_CURRENT_EVENT);
    }

    /**
     * Returns the object that fired the event. This is an IE-only property.
     * @return the object that fired the event
     */
    public Object jsxGet_srcElement() {
        return srcElement_;
    }

    /**
     * Sets the object that fired the event. This is an IE-only property.
     * @param srcElement the object that fired the event
     */
    public void jsxSet_srcElement(final Object srcElement) {
        srcElement_ = srcElement;
    }

    /**
     * Returns the event target to which the event was originally dispatched.
     * @return the event target to which the event was originally dispatched
     */
    public Object jsxGet_target() {
        return target_;
    }

    /**
     * Sets the event target.
     * @param target the event target
     */
    public void setTarget(final Object target) {
        target_ = target;
    }

    /**
     * Returns the event target whose event listeners are currently being processed. This
     * is useful during event capturing and event bubbling.
     * @return the current event target
     */
    public Object jsxGet_currentTarget() {
        return currentTarget_;
    }

    /**
     * Sets the current target.
     * @param target the new value
     */
    public void setCurrentTarget(final Scriptable target) {
        currentTarget_ = target;
    }

    /**
     * Returns the event type.
     * @return the event type
     */
    public String jsxGet_type() {
        return type_;
    }

    /**
     * Sets the event type.
     * @param eventType the event type
     */
    public void setEventType(final String eventType) {
        type_ = eventType;
    }

    /**
     * Returns the time at which this event was created.
     * @return the time at which this event was created
     */
    public long jsxGet_timeStamp() {
        return timeStamp_;
    }

    /**
     * Returns the key code associated with the event.
     * @return the key code associated with the event
     */
    public Object jsxGet_keyCode() {
        return keyCode_;
    }

    /**
     * @return whether SHIFT has been pressed during this event or not
     */
    public boolean jsxGet_shiftKey() {
        return shiftKey_;
    }

    /**
     * @param shiftKey whether SHIFT has been pressed during this event or not
     */
    protected void setShiftKey(final boolean shiftKey) {
        shiftKey_ = shiftKey;
    }

    /**
     * @return whether CTRL has been pressed during this event or not
     */
    public boolean jsxGet_ctrlKey() {
        return ctrlKey_;
    }

    /**
     * @param ctrlKey whether CTRL has been pressed during this event or not
     */
    protected void setCtrlKey(final boolean ctrlKey) {
        ctrlKey_ = ctrlKey;
    }

    /**
     * @return whether ALT has been pressed during this event or not
     */
    public boolean jsxGet_altKey() {
        return altKey_;
    }

    /**
     * @param altKey whether ALT has been pressed during this event or not
     */
    protected void setAltKey(final boolean altKey) {
        altKey_ = altKey;
    }

    /**
     * @return the current event phase for the event
     */
    public int jsxGet_eventPhase() {
        return eventPhase_;
    }

    /**
     * Sets the current event phase. Must be one of {@link #CAPTURING_PHASE}, {@link #AT_TARGET} or
     * {@link #BUBBLING_PHASE}.
     *
     * @param phase the phase the event is in
     */
    public void setEventPhase(final short phase) {
        if (phase != CAPTURING_PHASE && phase != AT_TARGET && phase != BUBBLING_PHASE) {
            throw new IllegalArgumentException("Illegal phase specified: " + phase);
        }
        eventPhase_ = phase;
    }

    /**
     * @return whether or not this event bubbles
     */
    public boolean jsxGet_bubbles() {
        return bubbles_;
    }

    /**
     * @return whether or not this event can be canceled
     */
    public boolean jsxGet_cancelable() {
        return cancelable_;
    }

    /**
     * @return indicates if event propagation is stopped
     */
    public boolean jsxGet_cancelBubble() {
        return stopPropagation_;
    }

    /**
     * @param newValue indicates if event propagation is stopped
     */
    public void jsxSet_cancelBubble(final boolean newValue) {
        stopPropagation_ = newValue;
    }

    /**
     * Stops the event from propagating.
     */
    public void jsxFunction_stopPropagation() {
        stopPropagation_ = true;
    }

    /**
     * Indicates if event propagation is stopped.
     * @return the status
     */
    public boolean isPropagationStopped() {
        return stopPropagation_;
    }

    /**
     * Returns the return value associated with the event.
     * @return the return value associated with the event
     */
    public Object jsxGet_returnValue() {
        return returnValue_;
    }

    /**
     * Returns the property name associated with the event.
     * @return the property name associated with the event
     */
    public String jsxGet_propertyName() {
        return propertyName_;
    }

    /**
     * Sets the return value associated with the event.
     * @param returnValue the return value associated with the event
     */
    public void jsxSet_returnValue(final Object returnValue) {
        returnValue_ = returnValue;
    }

    /**
     * Initializes this event.
     * @param type the event type
     * @param bubbles whether or not the event should bubble
     * @param cancelable whether or not the event the event should be cancelable
     */
    public void jsxFunction_initEvent(final String type, final boolean bubbles, final boolean cancelable) {
        type_ = type;
        bubbles_ = bubbles;
        cancelable_ = cancelable;
    }

    /**
     * If, during any stage of event flow, this method is called the event is canceled.
     * Any default action associated with the event will not occur.
     * Calling this method for a non-cancelable event has no effect.
     */
    public void jsxFunction_preventDefault() {
        preventDefault_ = true;
    }

    /**
     * Returns if the preventDefault() method has been called for this event.
     * @return if the preventDefault() method has been called for this event
     */
    public boolean isPreventDefault() {
        return preventDefault_;
    }

    /**
     * Copies properties from another event to this event. This method should
     * be overridden in subclasses in order to account for extra properties.
     * @param event the event to copy the properties from
     */
    public void copyPropertiesFrom(final Event event) {
        srcElement_ = event.srcElement_;
        target_ = event.target_;
        currentTarget_ = event.currentTarget_;
        type_ = event.type_;
        keyCode_ = event.keyCode_;
        shiftKey_ = event.shiftKey_;
        ctrlKey_ = event.ctrlKey_;
        altKey_ = event.altKey_;
        stopPropagation_ = event.stopPropagation_;
        returnValue_ = event.returnValue_;
        eventPhase_ = event.eventPhase_;
        bubbles_ = event.bubbles_;
        cancelable_ = event.cancelable_;

        // copy dynamic properties as well
        final List<Object> localIds = Arrays.asList(getAllIds());
        final List<Object> fromIds = Arrays.asList(event.getAllIds());
        for (final Object id : fromIds) {
            if (!localIds.contains(id)) {
                if (id instanceof String) {
                    putProperty(this, (String) id, getProperty(event, (String) id));
                }
                else {
                    putProperty(this, (Integer) id, getProperty(event, (Integer) id));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("Event ");
        buffer.append(jsxGet_type());
        buffer.append(" (");
        buffer.append("Current Target: ");
        buffer.append(currentTarget_);
        buffer.append(");");
        return buffer.toString();
    }

}

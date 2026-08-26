// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Splitter
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Provides the pointer- and keyboard-operable workspace splitter presentation control.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useCallback, useEffect, useRef, useState } from "react";
import type { KeyboardEvent, PointerEvent } from "react";

interface SplitterProperties
{
    readonly ariaLabel: string;
    readonly orientation: "horizontal" | "vertical";
    readonly value: number;
    readonly minimum: number;
    readonly maximum?: number;
    readonly opposingMinimum?: number;
    readonly direction?: 1 | -1;
    readonly className?: string;
    readonly onChange: ( value: number ) => void;
}

function clamp ( value: number, minimum: number, maximum: number ): number
{
    return Math.min ( maximum, Math.max ( minimum, value ) );
}

export function Splitter ( properties: SplitterProperties )
{
    const [ dragOrigin, setDragOrigin ] = useState <{ coordinate: number; value: number } | null> ( null );
    const [ measuredMaximum, setMeasuredMaximum ] = useState <number | null> ( null );
    const splitterReference = useRef <HTMLDivElement> ( null );
    const direction = properties.direction ?? 1;
    const maximum = properties.maximum ?? measuredMaximum ?? Math.max ( properties.minimum, properties.value );

    useEffect ( () =>
    {
        if ( properties.maximum !== undefined )
        {
            setMeasuredMaximum ( null );
            return undefined;
        }

        const splitter = splitterReference.current;
        const rangeContainer = splitter?.parentElement;

        if ( splitter === null || splitter === undefined || rangeContainer === null || rangeContainer === undefined )
        {
            return undefined;
        }

        function measureMaximum (): void
        {
            const range = properties.orientation === "vertical"
                ? rangeContainer!.clientWidth
                : rangeContainer!.clientHeight;
            const splitterSize = properties.orientation === "vertical"
                ? splitter!.clientWidth
                : splitter!.clientHeight;

            if ( range > 0 )
            {
                const fractionMaximum = Math.floor ( range * 2 / 3 );
                const opposingPaneMaximum = range - splitterSize - ( properties.opposingMinimum ?? 0 );

                setMeasuredMaximum ( Math.max (
                    properties.minimum,
                    Math.min ( fractionMaximum, opposingPaneMaximum )
                ) );
            }
        }

        measureMaximum ();

        if ( typeof ResizeObserver === "undefined" )
        {
            window.addEventListener ( "resize", measureMaximum );
            return () => window.removeEventListener ( "resize", measureMaximum );
        }

        const observer = new ResizeObserver ( measureMaximum );

        observer.observe ( rangeContainer );

        return () => observer.disconnect ();
    }, [ properties.maximum, properties.minimum, properties.opposingMinimum, properties.orientation ] );

    useEffect ( () =>
    {
        if ( properties.value < properties.minimum || properties.value > maximum )
        {
            properties.onChange ( clamp ( properties.value, properties.minimum, maximum ) );
        }
    }, [ maximum, properties ] );

    const stopDragging = useCallback (
        () =>
        {
            setDragOrigin ( null );
        },
        []
    );

    const moveSplitter = useCallback (
        ( event: globalThis.PointerEvent ) =>
        {
            if ( dragOrigin === null )
            {
                return;
            }

            const coordinate = properties.orientation === "vertical" ? event.clientX : event.clientY;
            const nextValue = dragOrigin.value + ( coordinate - dragOrigin.coordinate ) * direction;

            properties.onChange ( clamp ( nextValue, properties.minimum, maximum ) );
        },
        [ dragOrigin, direction, maximum, properties ]
    );

    useEffect (
        () =>
        {
            if ( dragOrigin === null )
            {
                return undefined;
            }

            window.addEventListener ( "pointermove", moveSplitter );
            window.addEventListener ( "pointerup", stopDragging );
            window.addEventListener ( "pointercancel", stopDragging );

            return () =>
            {
                window.removeEventListener ( "pointermove", moveSplitter );
                window.removeEventListener ( "pointerup", stopDragging );
                window.removeEventListener ( "pointercancel", stopDragging );
            };
        },
        [ dragOrigin, moveSplitter, stopDragging ]
    );

    function handlePointerDown ( event: PointerEvent <HTMLDivElement> ): void
    {
        event.currentTarget.setPointerCapture ( event.pointerId );
        setDragOrigin (
            {
                coordinate: properties.orientation === "vertical" ? event.clientX : event.clientY,
                value: properties.value,
            }
        );
    }

    function handleKeyDown ( event: KeyboardEvent <HTMLDivElement> ): void
    {
        let delta = 0;

        if ( properties.orientation === "vertical" )
        {
            if ( event.key === "ArrowLeft" )
            {
                delta = -10 * direction;
            }
            else if ( event.key === "ArrowRight" )
            {
                delta = 10 * direction;
            }
        }
        else if ( event.key === "ArrowUp" )
        {
            delta = -10 * direction;
        }
        else if ( event.key === "ArrowDown" )
        {
            delta = 10 * direction;
        }

        if ( event.key === "Home" )
        {
            event.preventDefault ();
            properties.onChange ( properties.minimum );
        }
        else if ( event.key === "End" )
        {
            event.preventDefault ();
            properties.onChange ( maximum );
        }
        else if ( delta !== 0 )
        {
            event.preventDefault ();
            properties.onChange (
                clamp ( properties.value + delta, properties.minimum, maximum )
            );
        }
    }

    const className = [ "splitter", `splitter-${properties.orientation}`, properties.className ]
        .filter ( Boolean )
        .join ( " " );

    return (
        <div
            aria-label={ properties.ariaLabel }
            aria-orientation={ properties.orientation }
            aria-valuemax={ maximum }
            aria-valuemin={ properties.minimum }
            aria-valuenow={ Math.round ( properties.value ) }
            className={ className }
            onKeyDown={ handleKeyDown }
            onPointerDown={ handlePointerDown }
            ref={ splitterReference }
            role="separator"
            tabIndex={ 0 }
        >
            <span aria-hidden="true" className="splitter-grip" />
        </div>
    );
}

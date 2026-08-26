// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    MenuBar
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Provides the desktop-parity accessible menu-bar presentation control.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { useEffect, useRef, useState } from "react";
import type { KeyboardEvent, MouseEvent } from "react";

import { Icon } from "./Icon";

export interface MenuIcon
{
    readonly name: string;
}

export type MenuEntry =
    | {
        readonly kind: "separator";
    }
    | {
        readonly kind: "item";
        readonly identifier: string;
        readonly label: string;
        readonly disabled?: boolean;
        readonly checked?: boolean;
        readonly checkRole?: "checkbox" | "radio";
        readonly children?: readonly MenuEntry[];
        readonly icon?: MenuIcon;
        readonly onSelect?: () => void;
        readonly shortcut?: string;
    };

export interface MenuDefinition
{
    readonly identifier: string;
    readonly label: string;
    readonly entries: readonly MenuEntry[];
}

interface MenuBarProperties
{
    readonly accessibleLabel: string;
    readonly menus: readonly MenuDefinition[];
}

interface MenuPopupProperties
{
    readonly entries: readonly MenuEntry[];
    readonly menuIdentifier: string;
    readonly nested?: boolean;
    readonly openSubmenu: string | null;
    readonly onOpenSubmenu: ( submenuIdentifier: string | null ) => void;
    readonly onClose: () => void;
    readonly onNavigateRoot?: ( offset: number ) => void;
    readonly onReturnFocus: () => void;
    readonly onSelectComplete: () => void;
}

function menuButtons ( element: HTMLElement ): HTMLButtonElement[]
{
    return Array.from ( element.children )
        .map (
            child => child instanceof HTMLButtonElement
                ? child
                : child.querySelector <HTMLButtonElement> ( ":scope > .menu-item" )
        )
        .filter (
            ( child ): child is HTMLButtonElement => child instanceof HTMLButtonElement && !child.disabled
        );
}

function focusRelativeItem ( event: KeyboardEvent <HTMLButtonElement>, offset: number ): void
{
    const menu = event.currentTarget.closest <HTMLElement> ( "[role='menu']" );

    if ( menu === null )
    {
        return;
    }

    const buttons = menuButtons ( menu );
    const currentIndex = buttons.indexOf ( event.currentTarget );
    const nextIndex = ( currentIndex + offset + buttons.length ) % buttons.length;

    event.preventDefault ();
    buttons [ nextIndex ]?.focus ();
}

function MenuPopup ( properties: MenuPopupProperties )
{
    function handleItemKeyDown (
        event: KeyboardEvent <HTMLButtonElement>,
        entry: Extract <MenuEntry, { kind: "item" }>
    ): void
    {
        if ( event.key === "ArrowDown" )
        {
            focusRelativeItem ( event, 1 );
        }
        else if ( event.key === "ArrowUp" )
        {
            focusRelativeItem ( event, -1 );
        }
        else if ( event.key === "Home" )
        {
            const parentElement = event.currentTarget.closest <HTMLElement> ( "[role='menu']" );

            event.preventDefault ();
            if ( parentElement !== null )
            {
                menuButtons ( parentElement ) [ 0 ]?.focus ();
            }
        }
        else if ( event.key === "End" )
        {
            const parentElement = event.currentTarget.closest <HTMLElement> ( "[role='menu']" );

            event.preventDefault ();
            if ( parentElement !== null )
            {
                menuButtons ( parentElement ).at ( -1 )?.focus ();
            }
        }
        else if ( properties.nested && ( event.key === "Escape" || event.key === "ArrowLeft" ) )
        {
            event.preventDefault ();
            properties.onOpenSubmenu ( null );
            properties.onReturnFocus ();
        }
        else if ( !properties.nested && event.key === "Escape" )
        {
            event.preventDefault ();
            properties.onClose ();
            properties.onReturnFocus ();
        }
        else if ( entry.children !== undefined && ( event.key === "ArrowRight" || event.key === "Enter" ) )
        {
            event.preventDefault ();
            properties.onOpenSubmenu ( entry.identifier );
            window.setTimeout (
                () =>
                {
                    const submenu = document.getElementById ( `${entry.identifier}-submenu` );
                    menuButtons ( submenu ?? document.body ) [ 0 ]?.focus ();
                },
                0
            );
        }
        else if ( entry.children === undefined && ( event.key === "Enter" || event.key === " " ) )
        {
            event.preventDefault ();
            entry.onSelect?.();
            properties.onClose ();
            properties.onSelectComplete ();
        }
        else if ( !properties.nested && ( event.key === "ArrowRight" || event.key === "ArrowLeft" ) )
        {
            event.preventDefault ();
            properties.onNavigateRoot?.( event.key === "ArrowRight" ? 1 : -1 );
        }
        else if ( event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey )
        {
            const parentElement = event.currentTarget.closest <HTMLElement> ( "[role='menu']" );
            const buttons = menuButtons ( parentElement ?? document.body );
            const searchCharacter = event.key.toLocaleLowerCase ();
            const currentIndex = buttons.indexOf ( event.currentTarget );
            const candidates = [ ...buttons.slice ( currentIndex + 1 ), ...buttons.slice ( 0, currentIndex + 1 ) ];
            const match = candidates.find (
                button => button.textContent?.trim ().toLocaleLowerCase ().startsWith ( searchCharacter )
            );

            if ( match !== undefined )
            {
                event.preventDefault ();
                match.focus ();
            }
        }
    }

    return (
        <div
            aria-label={ properties.menuIdentifier }
            className={ properties.nested ? "menu-popup menu-popup-nested" : "menu-popup" }
            id={ properties.nested ? `${properties.menuIdentifier}-submenu` : undefined }
            role="menu"
        >
            { properties.entries.map (
                ( entry, entryIndex ) =>
                {
                    if ( entry.kind === "separator" )
                    {
                        return <div className="menu-separator" key={ `separator-${entryIndex}` } role="separator" />;
                    }

                    const hasChildren = entry.children !== undefined;
                    const submenuOpen = properties.openSubmenu === entry.identifier;

                    return (
                        <div className="menu-item-container" key={ entry.identifier } role="none">
                            <button
                                aria-checked={ entry.checked === undefined ? undefined : entry.checked }
                                aria-disabled={ entry.disabled || undefined }
                                aria-expanded={ hasChildren ? submenuOpen : undefined }
                                aria-haspopup={ hasChildren ? "menu" : undefined }
                                className="menu-item"
                                data-menu-entry={ entry.identifier }
                                disabled={ entry.disabled }
                                onClick={ () =>
                                {
                                    if ( hasChildren )
                                    {
                                        properties.onOpenSubmenu ( submenuOpen ? null : entry.identifier );
                                    }
                                    else
                                    {
                                        entry.onSelect?.();
                                        properties.onClose ();
                                        properties.onSelectComplete ();
                                    }
                                } }
                                onKeyDown={ event => handleItemKeyDown ( event, entry ) }
                                onMouseEnter={ () =>
                                {
                                    if ( hasChildren )
                                    {
                                        properties.onOpenSubmenu ( entry.identifier );
                                    }
                                    else if ( !properties.nested )
                                    {
                                        properties.onOpenSubmenu ( null );
                                    }
                                } }
                                role={ entry.checked === undefined
                                    ? "menuitem"
                                    : entry.checkRole === "checkbox"
                                        ? "menuitemcheckbox"
                                        : "menuitemradio" }
                                tabIndex={ -1 }
                                type="button"
                            >
                                <span aria-hidden="true" className="menu-checkmark">
                                    { entry.checked ? "✓" : "" }
                                </span>
                                <span className="menu-icon-slot">
                                    { entry.icon !== undefined && <Icon name={ entry.icon.name } /> }
                                </span>
                                <span className="menu-label">{ entry.label }</span>
                                <span aria-hidden="true" className="menu-shortcut">{ entry.shortcut ?? "" }</span>
                                <span aria-hidden="true" className="menu-arrow">{ hasChildren ? "▶" : "" }</span>
                            </button>
                            { hasChildren && submenuOpen && (
                                <MenuPopup
                                    entries={ entry.children ?? [] }
                                    menuIdentifier={ entry.identifier }
                                    nested
                                    onClose={ properties.onClose }
                                    onOpenSubmenu={ properties.onOpenSubmenu }
                                    onReturnFocus={ () =>
                                    {
                                        document.querySelector <HTMLButtonElement> (
                                            `[data-menu-entry='${entry.identifier}']`
                                        )?.focus ();
                                    } }
                                    onSelectComplete={ properties.onSelectComplete }
                                    openSubmenu={ null }
                                />
                            ) }
                        </div>
                    );
                }
            ) }
        </div>
    );
}

export function MenuBar ( properties: MenuBarProperties )
{
    const [ openMenu, setOpenMenu ] = useState <string | null> ( null );
    const [ openSubmenu, setOpenSubmenu ] = useState <string | null> ( null );
    const [ activeMenuIndex, setActiveMenuIndex ] = useState ( 0 );
    const rootReference = useRef <HTMLDivElement> ( null );
    const menuButtonReferences = useRef <Map <string, HTMLButtonElement>> ( new Map () );

    useEffect (
        () =>
        {
            function closeFromOutside ( event: globalThis.PointerEvent ): void
            {
                if ( !rootReference.current?.contains ( event.target as Node ) )
                {
                    setOpenMenu ( null );
                    setOpenSubmenu ( null );
                }
            }

            window.addEventListener ( "pointerdown", closeFromOutside );

            return () => window.removeEventListener ( "pointerdown", closeFromOutside );
        },
        []
    );

    useEffect (
        () =>
        {
            function focusMenuBar ( event: globalThis.KeyboardEvent ): void
            {
                if ( event.key !== "F10" && event.key !== "Alt" )
                {
                    return;
                }

                event.preventDefault ();
                closeMenus ();
                focusMenuButton ( activeMenuIndex );
            }

            window.addEventListener ( "keydown", focusMenuBar );

            return () => window.removeEventListener ( "keydown", focusMenuBar );
        },
        [ activeMenuIndex ]
    );

    function closeMenus (): void
    {
        setOpenMenu ( null );
        setOpenSubmenu ( null );
    }

    function focusMenuButton ( menuIndex: number ): void
    {
        const normalizedIndex = ( menuIndex + properties.menus.length ) % properties.menus.length;
        const menu = properties.menus [ normalizedIndex ];

        if ( menu !== undefined )
        {
            setActiveMenuIndex ( normalizedIndex );
            menuButtonReferences.current.get ( menu.identifier )?.focus ();
        }
    }

    function navigateOpenMenu ( menuIndex: number, offset: number ): void
    {
        const normalizedIndex = ( menuIndex + offset + properties.menus.length ) % properties.menus.length;
        const menu = properties.menus [ normalizedIndex ];

        if ( menu === undefined )
        {
            return;
        }

        setActiveMenuIndex ( normalizedIndex );
        openMenuAndFocusFirstItem ( menu );
    }

    function openMenuAndFocusFirstItem ( menu: MenuDefinition ): void
    {
        setOpenMenu ( menu.identifier );
        setOpenSubmenu ( null );
        window.setTimeout (
            () =>
            {
                const menuElement = document.getElementById ( `${menu.identifier}-menu` )
                    ?.querySelector <HTMLElement> ( "[role='menu']" );
                menuButtons ( menuElement ?? document.body ) [ 0 ]?.focus ();
            },
            0
        );
    }

    function handleMenuButtonKeyDown (
        event: KeyboardEvent <HTMLButtonElement>,
        menu: MenuDefinition,
        menuIndex: number
    ): void
    {
        if ( event.key === "ArrowRight" || event.key === "ArrowLeft" )
        {
            event.preventDefault ();
            const nextIndex = event.key === "ArrowRight" ? menuIndex + 1 : menuIndex - 1;
            focusMenuButton ( nextIndex );

            if ( openMenu !== null )
            {
                const nextMenu = properties.menus [
                    ( nextIndex + properties.menus.length ) % properties.menus.length
                ];

                if ( nextMenu !== undefined )
                {
                    setOpenMenu ( nextMenu.identifier );
                    setOpenSubmenu ( null );
                }
            }
        }
        else if ( event.key === "ArrowDown" || event.key === "Enter" || event.key === " " )
        {
            event.preventDefault ();
            openMenuAndFocusFirstItem ( menu );
        }
        else if ( event.key === "Escape" )
        {
            closeMenus ();
        }
    }

    return (
        <nav aria-label={ properties.accessibleLabel } className="menu-region">
            <div aria-label={ properties.accessibleLabel } className="menu-bar" ref={ rootReference } role="menubar">
                { properties.menus.map (
                ( menu, menuIndex ) =>
                {
                    const menuOpen = openMenu === menu.identifier;

                    return (
                        <div className="menu-root" key={ menu.identifier }>
                            <button
                                aria-controls={ `${menu.identifier}-menu` }
                                aria-expanded={ menuOpen }
                                aria-haspopup="menu"
                                className="menu-root-button"
                                onClick={ ( event: MouseEvent <HTMLButtonElement> ) =>
                                {
                                    event.stopPropagation ();
                                    setActiveMenuIndex ( menuIndex );
                                    setOpenMenu ( menuOpen ? null : menu.identifier );
                                    setOpenSubmenu ( null );
                                } }
                                onFocus={ () => setActiveMenuIndex ( menuIndex ) }
                                onKeyDown={ event => handleMenuButtonKeyDown ( event, menu, menuIndex ) }
                                onMouseEnter={ () =>
                                {
                                    if ( openMenu !== null )
                                    {
                                        setOpenMenu ( menu.identifier );
                                        setOpenSubmenu ( null );
                                    }
                                } }
                                ref={ ( element: HTMLButtonElement | null ) =>
                                {
                                    if ( element === null )
                                    {
                                        menuButtonReferences.current.delete ( menu.identifier );
                                    }
                                    else
                                    {
                                        menuButtonReferences.current.set ( menu.identifier, element );
                                    }
                                } }
                                role="menuitem"
                                tabIndex={ activeMenuIndex === menuIndex ? 0 : -1 }
                                type="button"
                            >
                                { menu.label }
                            </button>
                            { menuOpen && (
                                <div id={ `${menu.identifier}-menu` }>
                                    <MenuPopup
                                        entries={ menu.entries }
                                        menuIdentifier={ menu.label }
                                        onClose={ closeMenus }
                                        onNavigateRoot={ offset => navigateOpenMenu ( menuIndex, offset ) }
                                        onOpenSubmenu={ setOpenSubmenu }
                                        onReturnFocus={ () =>
                                        {
                                            menuButtonReferences.current.get ( menu.identifier )?.focus ();
                                        } }
                                        onSelectComplete={ () =>
                                        {
                                            menuButtonReferences.current.get ( menu.identifier )?.focus ();
                                        } }
                                        openSubmenu={ openSubmenu }
                                    />
                                </div>
                            ) }
                        </div>
                    );
                }
                ) }
            </div>
        </nav>
    );
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    NavigationTree
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Provides the accessible Outline tree and keyboard navigation behavior.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import type { KeyboardEvent, MouseEvent, ReactNode } from "react";

import { text } from "../localization/messages";
import type { MessageKey } from "../localization/messages";

export type GuideContext =
    | "model"
    | "parameters"
    | "payloads"
    | "events"
    | "conditions"
    | "condition-sets"
    | "actions"
    | "rules"
    | "simulator";

interface NavigationItem
{
    readonly identifier: Exclude <GuideContext, "simulator">;
    readonly labelKey: MessageKey;
    readonly children?: readonly NavigationItem[];
}

interface NavigationTreeProperties
{
    readonly expandedItems: ReadonlySet <string>;
    readonly selectedItem: Exclude <GuideContext, "simulator">;
    readonly onExpandedItemsChange: ( expandedItems: ReadonlySet <string> ) => void;
    readonly onSelect: ( context: Exclude <GuideContext, "simulator"> ) => void;
}

const CATEGORY_ITEMS: readonly NavigationItem[] =
[
    { identifier: "parameters",     labelKey: "tree.parameters"     },
    { identifier: "payloads",       labelKey: "tree.payloads"       },
    { identifier: "events",         labelKey: "tree.events"         },
    { identifier: "conditions",     labelKey: "tree.conditions"     },
    { identifier: "condition-sets", labelKey: "tree.condition-sets" },
    { identifier: "actions",        labelKey: "tree.actions"        },
    { identifier: "rules",          labelKey: "tree.rules"          },
];

const MODEL_ITEM: NavigationItem =
{
    identifier: "model",
    labelKey: "tree.model",
    children: CATEGORY_ITEMS,
};

const TREE_NAVIGATION_KEYS = new Set (
    [ "ArrowDown", "ArrowUp", "Home", "End", "ArrowRight", "ArrowLeft", "Enter", " " ]
);

function visibleTreeItems ( treeItem: HTMLElement ): HTMLElement[]
{
    const tree = treeItem.closest <HTMLElement> ( "[role='tree']" );

    return Array.from ( tree?.querySelectorAll <HTMLElement> ( "[role='treeitem']" ) ?? [] );
}

export function NavigationTree ( properties: NavigationTreeProperties )
{
    function setExpanded ( identifier: string, expanded: boolean ): void
    {
        const nextExpandedItems = new Set ( properties.expandedItems );

        if ( expanded )
        {
            nextExpandedItems.add ( identifier );
        }
        else
        {
            nextExpandedItems.delete ( identifier );
        }

        properties.onExpandedItemsChange ( nextExpandedItems );
    }

    function selectElement ( treeItem: HTMLElement ): void
    {
        const identifier = treeItem.dataset.treeIdentifier as Exclude <GuideContext, "simulator"> | undefined;

        treeItem.focus ();
        if ( identifier !== undefined )
        {
            properties.onSelect ( identifier );
        }
    }

    function handleKeyDown ( event: KeyboardEvent <HTMLLIElement>, item: NavigationItem ): void
    {
        const currentElement = event.currentTarget;
        const treeItems = visibleTreeItems ( currentElement );
        const currentIndex = treeItems.indexOf ( currentElement );
        const expanded = properties.expandedItems.has ( item.identifier );
        const hasChildren = item.children !== undefined && item.children.length > 0;

        if ( TREE_NAVIGATION_KEYS.has ( event.key ) )
        {
            event.stopPropagation ();
        }

        if ( event.key === "ArrowDown" )
        {
            event.preventDefault ();
            const nextTreeItem = treeItems [ Math.min ( treeItems.length - 1, currentIndex + 1 ) ];
            if ( nextTreeItem !== undefined )
            {
                selectElement ( nextTreeItem );
            }
        }
        else if ( event.key === "ArrowUp" )
        {
            event.preventDefault ();
            const previousTreeItem = treeItems [ Math.max ( 0, currentIndex - 1 ) ];
            if ( previousTreeItem !== undefined )
            {
                selectElement ( previousTreeItem );
            }
        }
        else if ( event.key === "Home" )
        {
            event.preventDefault ();
            const firstTreeItem = treeItems [ 0 ];
            if ( firstTreeItem !== undefined )
            {
                selectElement ( firstTreeItem );
            }
        }
        else if ( event.key === "End" )
        {
            event.preventDefault ();
            const lastTreeItem = treeItems.at ( -1 );
            if ( lastTreeItem !== undefined )
            {
                selectElement ( lastTreeItem );
            }
        }
        else if ( event.key === "ArrowRight" && hasChildren )
        {
            event.preventDefault ();
            if ( !expanded )
            {
                setExpanded ( item.identifier, true );
            }
            else
            {
                const firstChild = currentElement.querySelector <HTMLElement> (
                    ":scope > [role='group'] > [role='treeitem']"
                );
                if ( firstChild !== null )
                {
                    selectElement ( firstChild );
                }
            }
        }
        else if ( event.key === "ArrowLeft" )
        {
            event.preventDefault ();
            if ( hasChildren && expanded )
            {
                setExpanded ( item.identifier, false );
            }
            else
            {
                const parentTreeItem = currentElement.parentElement?.closest <HTMLElement> ( "[role='treeitem']" );
                if ( parentTreeItem !== null && parentTreeItem !== undefined )
                {
                    selectElement ( parentTreeItem );
                }
            }
        }
        else if ( event.key === "Enter" || event.key === " " )
        {
            event.preventDefault ();
            properties.onSelect ( item.identifier );
        }
    }

    function renderItem ( item: NavigationItem ): ReactNode
    {
        const hasChildren = item.children !== undefined && item.children.length > 0;
        const expanded = hasChildren && properties.expandedItems.has ( item.identifier );
        const selected = properties.selectedItem === item.identifier;

        return (
            <li
                aria-expanded={ hasChildren ? expanded : undefined }
                aria-label={ text ( item.labelKey ) }
                aria-selected={ selected }
                data-tree-identifier={ item.identifier }
                key={ item.identifier }
                onClick={ event =>
                {
                    event.stopPropagation ();
                    selectElement ( event.currentTarget );
                } }
                onKeyDown={ event => handleKeyDown ( event, item ) }
                role="treeitem"
                tabIndex={ selected ? 0 : -1 }
            >
                <div className={ selected ? "tree-row tree-row-selected" : "tree-row" }>
                    <span
                        aria-hidden="true"
                        className={ hasChildren ? "tree-disclosure" : "tree-disclosure tree-disclosure-empty" }
                        onClick={ ( event: MouseEvent <HTMLSpanElement> ) =>
                        {
                            event.stopPropagation ();
                            if ( hasChildren )
                            {
                                setExpanded ( item.identifier, !expanded );
                            }
                        } }
                    >
                        { hasChildren ? ( expanded ? "▼" : "▶" ) : "" }
                    </span>
                    <span>{ text ( item.labelKey ) }</span>
                </div>
                { hasChildren && expanded && (
                    <ul role="group">
                        { item.children?.map ( renderItem ) }
                    </ul>
                ) }
            </li>
        );
    }

    return (
        <>
            <p className="visually-hidden" id="outline-instructions">
                { text ( "web.tree.instructions" ) }
            </p>
            <ul
                aria-describedby="outline-instructions"
                aria-label={ text ( "outline.title" ) }
                className="tree"
                role="tree"
            >
                { renderItem ( MODEL_ITEM ) }
            </ul>
        </>
    );
}

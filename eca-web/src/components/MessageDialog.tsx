// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Message Dialog
// Version: 2.0.0
// Date:    2026-08-14
// Author:  Rohin Gosling
//
// Description:
//
//   Presents operational messages using the shared Automata-style modal shell.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { text } from "../localization/messages";
import type { ConsoleSeverity } from "./ConsolePanel";
import { ModalDialog } from "./ModalDialog";

interface MessageDialogProperties
{
    readonly body:     string;
    readonly onClose:  () => void;
    readonly open:     boolean;
    readonly severity: ConsoleSeverity;
}

export function MessageDialog ( properties: MessageDialogProperties )
{
    const title = text ( `web.console.severity.${properties.severity}` );
    const symbol = properties.severity === "error" ? "E" : properties.severity === "warning" ? "W" : "M";

    return (
        <ModalDialog
            actions={
                <button onClick={ properties.onClose } type="button">{ text ( "button.ok" ) }</button>
            }
            initialFocusSelector=".dialog-footer button"
            onRequestClose={ properties.onClose }
            open={ properties.open }
            title={ title }
            titleIdentifier="message-dialog-title"
        >
            <div className={ `message-dialog-body message-dialog-${properties.severity}` }>
                <span aria-hidden="true" className="severity-symbol">{ symbol }</span>
                <p>{ properties.body }</p>
            </div>
        </ModalDialog>
    );
}

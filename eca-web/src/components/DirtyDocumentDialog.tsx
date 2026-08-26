// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Dirty Document Dialog
// Version: 2.0.0
// Date:    2026-08-14
// Author:  Rohin Gosling
//
// Description:
//
//   Confirms replacement of a modified browser document with the shared modal interaction pattern.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import { text } from "../localization/messages";
import { ModalDialog } from "./ModalDialog";

interface DirtyDocumentDialogProperties
{
    readonly canSave:    boolean;
    readonly onCancel:   () => void;
    readonly onDiscard:  () => void;
    readonly onSave:     () => void;
    readonly open:       boolean;
}

export function DirtyDocumentDialog ( properties: DirtyDocumentDialogProperties )
{
    return (
        <ModalDialog
            actions={
                <>
                    <button onClick={ properties.onCancel } type="button">{ text ( "button.cancel" ) }</button>
                    { properties.canSave && (
                        <button onClick={ properties.onSave } type="button">
                            { text ( "web.button.save.and.continue" ) }
                        </button>
                    ) }
                    <button className="danger-button" onClick={ properties.onDiscard } type="button">
                        { text ( "web.button.discard.and.continue" ) }
                    </button>
                </>
            }
            initialFocusSelector=".dialog-footer button"
            onRequestClose={ properties.onCancel }
            open={ properties.open }
            title={ text ( "dialog.dirty.title" ) }
            titleIdentifier="dirty-document-dialog-title"
        >
            <p>{ text ( "dialog.dirty.description" ) }</p>
        </ModalDialog>
    );
}

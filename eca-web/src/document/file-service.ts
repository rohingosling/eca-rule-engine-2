// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    file-service
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Provides browser-local model selection, retained file-handle writes, and truthful download fallback behavior.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

export interface DocumentWritableStream
{
    abort?: () => Promise <void>;
    close: () => Promise <void>;
    write: ( contents: string ) => Promise <void>;
}

export interface DocumentFileHandle
{
    readonly name: string;
    createWritable: () => Promise <DocumentWritableStream>;
    getFile: () => Promise <File>;
}

export interface OpenedDocument
{
    readonly contents: string;
    readonly fileHandle: DocumentFileHandle | null;
    readonly fileName: string;
}

export interface SavedDocument
{
    readonly fileHandle: DocumentFileHandle | null;
    readonly fileName: string;
    readonly kind: "download" | "file-handle";
}

export interface BrowserFileEnvironment
{
    chooseFallbackFile: () => Promise <File | null>;
    download: ( fileName: string, contents: string ) => void;
    showOpenFilePicker?: () => Promise <readonly DocumentFileHandle[]>;
    showSaveFilePicker?: ( suggestedName: string ) => Promise <DocumentFileHandle>;
}

function chooseFallbackFile (): Promise <File | null>
{
    return new Promise (
        resolve =>
        {
            const input = document.createElement ( "input" );
            let settled = false;

            function finish ( file: File | null ): void
            {
                if ( settled )
                {
                    return;
                }

                settled = true;
                input.remove ();
                resolve ( file );
            }

            input.accept = ".json,application/json";
            input.type = "file";
            input.addEventListener ( "change", () => finish ( input.files?.item ( 0 ) ?? null ) );
            input.addEventListener ( "cancel", () => finish ( null ) );
            input.style.display = "none";
            document.body.append ( input );
            input.click ();
        }
    );
}

function download ( fileName: string, contents: string ): void
{
    const blob = new Blob ( [ contents ], { type: "application/json;charset=utf-8" } );
    const objectURL = URL.createObjectURL ( blob );
    const anchor = document.createElement ( "a" );

    anchor.download = fileName;
    anchor.href = objectURL;
    anchor.style.display = "none";
    document.body.append ( anchor );
    anchor.click ();
    anchor.remove ();
    window.setTimeout ( () => URL.revokeObjectURL ( objectURL ), 0 );
}

function defaultEnvironment (): BrowserFileEnvironment
{
    const browserWindow = window as typeof window & {
        showOpenFilePicker?: ( options: unknown ) => Promise <readonly DocumentFileHandle[]>;
        showSaveFilePicker?: ( options: unknown ) => Promise <DocumentFileHandle>;
    };
    const openFilePicker = browserWindow.showOpenFilePicker;
    const saveFilePicker = browserWindow.showSaveFilePicker;

    return {
        chooseFallbackFile,
        download,
        showOpenFilePicker: openFilePicker === undefined
            ? undefined
            : () => openFilePicker (
                {
                    excludeAcceptAllOption: true,
                    multiple: false,
                    types:
                    [
                        {
                            accept: { "application/json": [ ".json" ] },
                            description: "ECA JSON Model",
                        },
                    ],
                }
            ),
        showSaveFilePicker: saveFilePicker === undefined
            ? undefined
            : suggestedName => saveFilePicker (
                {
                    excludeAcceptAllOption: true,
                    suggestedName,
                    types:
                    [
                        {
                            accept: { "application/json": [ ".json" ] },
                            description: "ECA JSON Model",
                        },
                    ],
                }
            ),
    };
}

async function writeFileHandle ( fileHandle: DocumentFileHandle, contents: string ): Promise <void>
{
    const writableStream = await fileHandle.createWritable ();

    try
    {
        await writableStream.write ( contents );
        await writableStream.close ();
    }
    catch ( error )
    {
        await writableStream.abort?.().catch ( () => undefined );
        throw error;
    }
}

export class BrowserDocumentFileService
{
    private readonly environment: BrowserFileEnvironment;

    public constructor ( environment: BrowserFileEnvironment = defaultEnvironment () )
    {
        this.environment = environment;
    }

    public async open (): Promise <OpenedDocument | null>
    {
        if ( this.environment.showOpenFilePicker !== undefined )
        {
            const fileHandles = await this.environment.showOpenFilePicker ();
            const fileHandle = fileHandles [ 0 ];

            if ( fileHandle === undefined )
            {
                return null;
            }

            const file = await fileHandle.getFile ();

            return {
                contents: await file.text (),
                fileHandle,
                fileName: file.name,
            };
        }

        const file = await this.environment.chooseFallbackFile ();

        if ( file === null )
        {
            return null;
        }

        return {
            contents: await file.text (),
            fileHandle: null,
            fileName: file.name,
        };
    }

    public async save (
        contents: string,
        suggestedName: string,
        retainedFileHandle: DocumentFileHandle | null,
        saveAs: boolean
    ): Promise <SavedDocument>
    {
        if ( !saveAs && retainedFileHandle !== null )
        {
            await writeFileHandle ( retainedFileHandle, contents );

            return {
                fileHandle: retainedFileHandle,
                fileName: retainedFileHandle.name,
                kind: "file-handle",
            };
        }

        if ( this.environment.showSaveFilePicker !== undefined )
        {
            const fileHandle = await this.environment.showSaveFilePicker ( suggestedName );

            await writeFileHandle ( fileHandle, contents );

            return {
                fileHandle,
                fileName: fileHandle.name,
                kind: "file-handle",
            };
        }

        this.environment.download ( suggestedName, contents );

        return {
            fileHandle: null,
            fileName: suggestedName,
            kind: "download",
        };
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    Icon
// Version: 2.0.0
// Date:    2026-08-14
// Author:  Rohin Gosling
//
// Description:
//
//   Resolves the curated, same-origin Microsoft Fluent SVG assets beneath the configured application base path.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

interface IconProperties
{
    readonly className?: string;
    readonly name:       string;
}

export function Icon ( properties: IconProperties )
{
    return (
        <img
            alt=""
            aria-hidden="true"
            className={ properties.className ?? "command-icon" }
            draggable={ false }
            src={ `${import.meta.env.BASE_URL}icons/fluent/${properties.name}` }
        />
    );
}

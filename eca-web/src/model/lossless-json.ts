// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    lossless-json
// Version: 2.0.0
// Date:    2026-08-02
// Author:  Rohin Gosling
//
// Description:
//
//   Parses strict JSON without collapsing duplicate members or exact number lexemes into JavaScript numbers.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import type { AuthoringValue, ExactJsonNumber } from "./types";

export interface LosslessJsonMember
{
    readonly name: string;
    readonly value: LosslessJsonValue;
}

export interface LosslessJsonObject
{
    readonly kind: "object";
    readonly members: readonly LosslessJsonMember[];
}

export interface LosslessJsonArray
{
    readonly kind: "array";
    readonly values: readonly LosslessJsonValue[];
}

export type LosslessJsonValue =
    | string
    | boolean
    | null
    | ExactJsonNumber
    | LosslessJsonObject
    | LosslessJsonArray;

const SIGNED_LONG_MINIMUM = -9223372036854775808n;
const SIGNED_LONG_MAXIMUM = 9223372036854775807n;

class LosslessJsonParser
{
    private readonly document: string;
    private position = 0;

    public constructor ( document: string )
    {
        this.document = document;
    }

    public parse (): LosslessJsonValue
    {
        if ( this.document.charCodeAt ( 0 ) === 0xfeff )
        {
            this.position++;
        }

        this.skipWhitespace ();

        const value = this.parseValue ( "$" );

        this.skipWhitespace ();

        if ( this.position !== this.document.length )
        {
            this.fail ( "The document contains trailing content." );
        }

        return value;
    }

    private parseValue ( path: string ): LosslessJsonValue
    {
        const character = this.document [ this.position ];

        if ( character === "{" )
        {
            return this.parseObject ( path );
        }

        if ( character === "[" )
        {
            return this.parseArray ( path );
        }

        if ( character === "\"" )
        {
            return this.parseString ();
        }

        if ( character === "t" )
        {
            this.consumeKeyword ( "true" );
            return true;
        }

        if ( character === "f" )
        {
            this.consumeKeyword ( "false" );
            return false;
        }

        if ( character === "n" )
        {
            this.consumeKeyword ( "null" );
            return null;
        }

        if ( character === "-" || isDecimalDigit ( character ) )
        {
            return this.parseNumber ();
        }

        this.fail ( `${path} does not contain a JSON value.` );
    }

    private parseObject ( path: string ): LosslessJsonObject
    {
        const members: LosslessJsonMember[] = [];
        const encounteredNames = new Set <string> ();

        this.position++;
        this.skipWhitespace ();

        if ( this.document [ this.position ] === "}" )
        {
            this.position++;
            return { kind: "object", members };
        }

        while ( true )
        {
            if ( this.document [ this.position ] !== "\"" )
            {
                this.fail ( `${path} must contain string member names.` );
            }

            const name = this.parseString ();

            if ( encounteredNames.has ( name ) )
            {
                this.fail ( `${path} contains duplicate member ${name}.` );
            }

            encounteredNames.add ( name );
            this.skipWhitespace ();

            if ( this.document [ this.position ] !== ":" )
            {
                this.fail ( `${path}.${name} is missing its member separator.` );
            }

            this.position++;
            this.skipWhitespace ();
            members.push ( { name, value: this.parseValue ( `${path}.${name}` ) } );
            this.skipWhitespace ();

            const separator = this.document [ this.position ];

            if ( separator === "}" )
            {
                this.position++;
                return { kind: "object", members };
            }

            if ( separator !== "," )
            {
                this.fail ( `${path} contains an invalid object separator.` );
            }

            this.position++;
            this.skipWhitespace ();
        }
    }

    private parseArray ( path: string ): LosslessJsonArray
    {
        const values: LosslessJsonValue[] = [];

        this.position++;
        this.skipWhitespace ();

        if ( this.document [ this.position ] === "]" )
        {
            this.position++;
            return { kind: "array", values };
        }

        while ( true )
        {
            values.push ( this.parseValue ( `${path}[${values.length}]` ) );
            this.skipWhitespace ();

            const separator = this.document [ this.position ];

            if ( separator === "]" )
            {
                this.position++;
                return { kind: "array", values };
            }

            if ( separator !== "," )
            {
                this.fail ( `${path} contains an invalid array separator.` );
            }

            this.position++;
            this.skipWhitespace ();
        }
    }

    private parseString (): string
    {
        let value = "";

        this.position++;

        while ( this.position < this.document.length )
        {
            const character = this.document [ this.position ] ?? "";

            this.position++;

            if ( character === "\"" )
            {
                return value;
            }

            if ( character === "\\" )
            {
                value += this.parseEscapeSequence ();
                continue;
            }

            if ( character.charCodeAt ( 0 ) < 0x20 )
            {
                this.fail ( "JSON strings must not contain unescaped control characters." );
            }

            value += character;
        }

        this.fail ( "The document contains an unterminated JSON string." );
    }

    private parseEscapeSequence (): string
    {
        const escapedCharacter = this.document [ this.position ] ?? "";

        this.position++;

        switch ( escapedCharacter )
        {
            case "\"": return "\"";
            case "\\": return "\\";
            case "/":  return "/";
            case "b":  return "\b";
            case "f":  return "\f";
            case "n":  return "\n";
            case "r":  return "\r";
            case "t":  return "\t";
            case "u":  return this.parseUnicodeEscape ();
            default:    this.fail ( "The document contains an invalid JSON escape sequence." );
        }
    }

    private parseUnicodeEscape (): string
    {
        const hexadecimalValue = this.document.slice ( this.position, this.position + 4 );

        if ( !/^[0-9a-fA-F]{4}$/.test ( hexadecimalValue ) )
        {
            this.fail ( "The document contains an invalid Unicode escape sequence." );
        }

        this.position += 4;

        return String.fromCharCode ( Number.parseInt ( hexadecimalValue, 16 ) );
    }

    private parseNumber (): ExactJsonNumber
    {
        const start = this.position;

        if ( this.document [ this.position ] === "-" )
        {
            this.position++;
        }

        if ( this.document [ this.position ] === "0" )
        {
            this.position++;

            if ( isDecimalDigit ( this.document [ this.position ] ) )
            {
                this.fail ( "JSON numbers must not contain leading zeroes." );
            }
        }
        else
        {
            this.consumeDigits ( true );
        }

        if ( this.document [ this.position ] === "." )
        {
            this.position++;
            this.consumeDigits ( true );
        }

        const exponentMarker = this.document [ this.position ];

        if ( exponentMarker === "e" || exponentMarker === "E" )
        {
            this.position++;

            const exponentSign = this.document [ this.position ];

            if ( exponentSign === "+" || exponentSign === "-" )
            {
                this.position++;
            }

            this.consumeDigits ( true );
        }

        return {
            kind: "exact-json-number",
            lexeme: this.document.slice ( start, this.position ),
        };
    }

    private consumeDigits ( requireAtLeastOne: boolean ): void
    {
        const start = this.position;

        while ( isDecimalDigit ( this.document [ this.position ] ) )
        {
            this.position++;
        }

        if ( requireAtLeastOne && start === this.position )
        {
            this.fail ( "The document contains an invalid JSON number." );
        }
    }

    private consumeKeyword ( keyword: string ): void
    {
        if ( this.document.slice ( this.position, this.position + keyword.length ) !== keyword )
        {
            this.fail ( "The document contains an invalid JSON keyword." );
        }

        this.position += keyword.length;
    }

    private skipWhitespace (): void
    {
        while ( true )
        {
            const character = this.document [ this.position ];

            if ( character !== " " && character !== "\t" && character !== "\r" && character !== "\n" )
            {
                return;
            }

            this.position++;
        }
    }

    private fail ( message: string ): never
    {
        throw new Error ( `${message} Position: ${this.position}.` );
    }
}

function isDecimalDigit ( character: string | undefined ): boolean
{
    return character !== undefined && character >= "0" && character <= "9";
}

function escapeJsonString ( value: string ): string
{
    return JSON.stringify ( value );
}

function indentation ( depth: number ): string
{
    return "  ".repeat ( depth );
}

function serializeJsonValue ( value: LosslessJsonValue, pretty: boolean, depth: number ): string
{
    if ( value === null )
    {
        return "null";
    }

    if ( typeof value === "string" )
    {
        return escapeJsonString ( value );
    }

    if ( typeof value === "boolean" )
    {
        return String ( value );
    }

    if ( value.kind === "exact-json-number" )
    {
        return value.lexeme;
    }

    if ( value.kind === "array" )
    {
        if ( value.values.length === 0 )
        {
            return pretty ? "[ ]" : "[]";
        }

        const separator = pretty ? ", " : ",";
        const elements = value.values.map ( element => serializeJsonValue ( element, pretty, depth ) );

        return pretty ? `[ ${elements.join ( separator )} ]` : `[${elements.join ( separator )}]`;
    }

    if ( value.members.length === 0 )
    {
        return pretty ? "{ }" : "{}";
    }

    if ( !pretty )
    {
        const members = value.members.map (
            member => `${escapeJsonString ( member.name )}:${serializeJsonValue ( member.value, false, depth + 1 )}`
        );

        return `{${members.join ( "," )}}`;
    }

    const members = value.members.map (
        member =>
            `${indentation ( depth + 1 )}${escapeJsonString ( member.name )} : `
                + serializeJsonValue ( member.value, true, depth + 1 )
    );

    return `{\n${members.join ( ",\n" )}\n${indentation ( depth )}}`;
}

export function parseLosslessJson ( document: string ): LosslessJsonValue
{
    return new LosslessJsonParser ( document ).parse ();
}

export function serializeLosslessJson ( value: LosslessJsonValue, pretty: boolean ): string
{
    return serializeJsonValue ( value, pretty, 0 ) + ( pretty ? "\n" : "" );
}

export function requireJsonObject ( value: LosslessJsonValue, path: string ): LosslessJsonObject
{
    if ( typeof value !== "object" || value === null || value.kind !== "object" )
    {
        throw new Error ( `${path} must be an object.` );
    }

    return value;
}

export function requireJsonArray ( value: LosslessJsonValue, path: string ): LosslessJsonArray
{
    if ( typeof value !== "object" || value === null || value.kind !== "array" )
    {
        throw new Error ( `${path} must be an array.` );
    }

    return value;
}

export function requireJsonString ( value: LosslessJsonValue | undefined, path: string ): string
{
    if ( typeof value !== "string" )
    {
        throw new Error ( `${path} must be a string.` );
    }

    return value;
}

export function findJsonMember ( object: LosslessJsonObject, name: string ): LosslessJsonValue | undefined
{
    return object.members.find ( member => member.name === name )?.value;
}

export function hasJsonMember ( object: LosslessJsonObject, name: string ): boolean
{
    return object.members.some ( member => member.name === name );
}

export function rejectUnknownJsonMembers (
    object: LosslessJsonObject,
    allowedNames: ReadonlySet <string>,
    path: string
): void
{
    for ( const member of object.members )
    {
        if ( !allowedNames.has ( member.name ) )
        {
            throw new Error ( `${path} contains unknown field ${member.name}.` );
        }
    }
}

export function exactJsonNumberToSignedLong ( value: ExactJsonNumber ): bigint | undefined
{
    const match = /^(-?)(0|[1-9][0-9]*)(?:\.([0-9]+))?(?:[eE]([+-]?[0-9]+))?$/.exec ( value.lexeme );

    if ( match === null )
    {
        return undefined;
    }

    const negative = match [ 1 ] === "-";
    const integerDigits = match [ 2 ] ?? "0";
    const fractionDigits = match [ 3 ] ?? "";
    const exponent = BigInt ( match [ 4 ] ?? "0" );
    const coefficientDigits = ( integerDigits + fractionDigits ).replace ( /^0+/, "" );

    if ( coefficientDigits.length === 0 )
    {
        return 0n;
    }

    const scale = BigInt ( fractionDigits.length ) - exponent;
    let integralDigits: string;

    if ( scale <= 0n )
    {
        const appendedZeroCount = -scale;

        if ( BigInt ( coefficientDigits.length ) + appendedZeroCount > 19n )
        {
            return undefined;
        }

        integralDigits = coefficientDigits + "0".repeat ( Number ( appendedZeroCount ) );
    }
    else
    {
        if ( scale > BigInt ( coefficientDigits.length ) )
        {
            return undefined;
        }

        const removedDigitCount = Number ( scale );
        const removedDigits = coefficientDigits.slice ( coefficientDigits.length - removedDigitCount );

        if ( /[^0]/.test ( removedDigits ) )
        {
            return undefined;
        }

        integralDigits = coefficientDigits.slice ( 0, coefficientDigits.length - removedDigitCount ) || "0";
    }

    const unsignedValue = BigInt ( integralDigits );
    const signedValue = negative ? -unsignedValue : unsignedValue;

    return signedValue >= SIGNED_LONG_MINIMUM && signedValue <= SIGNED_LONG_MAXIMUM
        ? signedValue
        : undefined;
}

export function isExactJsonNumber ( value: AuthoringValue ): value is ExactJsonNumber
{
    return typeof value === "object" && value !== null && "kind" in value && value.kind === "exact-json-number";
}

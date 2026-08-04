// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Name:    immutable-map
// Version: 2.0.0
// Date:    2026-08-03
// Author:  Rohin Gosling
//
// Description:
//
//   Provides a copied read-only map without exposing the mutators present on the JavaScript Map implementation.
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

export class ImmutableMap <Key, Value> implements ReadonlyMap <Key, Value>
{
    private readonly backingMap: Map <Key, Value>;

    public get size (): number
    {
        return this.backingMap.size;
    }

    public get [ Symbol.toStringTag ] (): string
    {
        return "ImmutableMap";
    }

    public constructor ( entries: Iterable <readonly [ Key, Value ]> )
    {
        this.backingMap = new Map ( entries );

        Object.freeze ( this );
    }

    public entries ()
    {
        return this.backingMap.entries ();
    }

    public forEach (
        callback: ( value: Value, key: Key, map: ReadonlyMap <Key, Value> ) => void,
        thisArgument?: unknown
    ): void
    {
        for ( const [ key, value ] of this.backingMap )
        {
            callback.call ( thisArgument, value, key, this );
        }
    }

    public get ( key: Key ): Value | undefined
    {
        return this.backingMap.get ( key );
    }

    public has ( key: Key ): boolean
    {
        return this.backingMap.has ( key );
    }

    public keys ()
    {
        return this.backingMap.keys ();
    }

    public values ()
    {
        return this.backingMap.values ();
    }

    public [ Symbol.iterator ] ()
    {
        return this.entries ();
    }
}

export function immutableMap <Key, Value> (
    entries: Iterable <readonly [ Key, Value ]>
): ReadonlyMap <Key, Value>
{
    return new ImmutableMap ( entries );
}

#---------------------------------------------------------------------------------------------------------------------
# Project: eca-rule-engine-2
# File:    generate_paper.py
# Version: 2.0
# Date:    2025
# Author:  Rohin Gosling
#
# Description:
#
#   Converts the stateless ECA rule-engine technical note from its constrained Markdown source format to the
#   project's styled LaTeX format. The converter uses only the Python standard library.
#
#---------------------------------------------------------------------------------------------------------------------

from __future__ import annotations

from argparse    import ArgumentParser, Namespace
from dataclasses import dataclass
from pathlib     import Path
from re          import compile as compile_regular_expression, fullmatch
from textwrap    import TextWrapper


PAPER_YEAR = "2026"

BLOCK_HEADING   = "heading"
BLOCK_LIST      = "list"
BLOCK_MATH      = "math"
BLOCK_PARAGRAPH = "paragraph"
BLOCK_QUOTE     = "quote"
BLOCK_TABLE     = "table"

# Prepare the list pattern, heading pattern, link pattern, citation pattern, citation number pattern, number prefix,
# table rule pattern, and paragraph wrapper values required by the module operation.

LIST_PATTERN            = compile_regular_expression ( r"^(\s*)([-+*]|\d+\.)\s+(.*)$" )
HEADING_PATTERN         = compile_regular_expression ( r"^(#{2,6})\s+(.+)$" )
LINK_PATTERN            = compile_regular_expression ( r"\[([^\]]+)\]\(([^)]+)\)" )
CITATION_PATTERN        = compile_regular_expression ( r"\[\d+\](?:,\s*\[\d+\])*" )
CITATION_NUMBER_PATTERN = compile_regular_expression ( r"\d+" )
NUMBER_PREFIX           = compile_regular_expression ( r"^\d+(?:\.\d+)*\.?\s+" )
TABLE_RULE_PATTERN      = compile_regular_expression ( r"^:?-{3,}:?$" )

PARAGRAPH_WRAPPER = TextWrapper (
    width              = 100,
    break_long_words   = False,
    break_on_hyphens   = False,
    replace_whitespace = False,
)

DOCUMENT_PREAMBLE = r"""\documentclass[11pt,letterpaper]{article}

\usepackage[T1]{fontenc}
\usepackage{newtxtext}
\usepackage{microtype}
\usepackage[letterpaper,margin=0.8in]{geometry}
\usepackage{amsmath,amssymb,amsthm,mathtools}
\usepackage{newtxmath}
\usepackage{booktabs,tabularx,array}
\usepackage{fancyhdr}
\usepackage{hyperref}
\usepackage{needspace}

\hypersetup{
    hidelinks,
    pdftitle    = {<<PDF_TITLE>>},
    pdfauthor   = {<<PDF_AUTHOR>>},
    pdfsubject  = {Independent technical note on a minimal, domain-independent stateless event-condition-action rule engine},
    pdfkeywords = {<<PDF_KEYWORDS>>},
    pdfcreator  = {LaTeX with hyperref}
}

\pagestyle{fancy}
\fancyhf{}
\fancyfoot[L]{\small Stateless Event-Condition-Action Rule Engine}
\fancyfoot[R]{\small Page \thepage}
\renewcommand{\headrulewidth}{0pt}
\renewcommand{\footrulewidth}{0.4pt}
\setlength{\headheight}{14pt}

\newtheorem{definition}{Definition}[section]
\newtheorem{proposition}[definition]{Proposition}

\newcolumntype{Y}{>{\raggedright\arraybackslash}X}
\newcolumntype{C}[1]{>{\centering\arraybackslash}p{#1}}
\renewcommand{\arraystretch}{1.15}
\setlength{\parskip}{0.4em}
\setlength{\parindent}{1.25em}
\emergencystretch=2em
\clubpenalty=10000
\widowpenalty=10000
\displaywidowpenalty=10000

% Keep headings and formal blocks with a meaningful amount of following content.
\AddToHook{cmd/section/before}{\Needspace{14\baselineskip}}
\AddToHook{cmd/subsection/before}{\Needspace{10\baselineskip}}
\AddToHook{env/definition/before}{\Needspace{8\baselineskip}}
\AddToHook{env/proposition/before}{\Needspace{12\baselineskip}}
\AddToHook{env/proof/before}{\Needspace{5\baselineskip}}

\renewenvironment{abstract}
    {\section*{Abstract}}
    {}

\begin{document}
"""

DOCUMENT_END = r"""\end{document}
"""


@dataclass ( frozen = True )
class PaperMetadata:

    title:      str
    subtitle:   str
    author:     str
    descriptor: str
    status:     str


@dataclass ( frozen = True )
class MarkdownBlock:

    kind:    str
    text:    str                               = ""
    level:   int                               = 0
    items:   tuple [ str, ... ]                = ()
    ordered: bool                              = False
    rows:    tuple [ tuple [ str, ... ], ... ] = ()


@dataclass ( frozen = True )
class TableProfile:

    required_space: int
    font_command:   str
    width:          str
    columns:        str

# Initialize the table profiles by applying table profile.

TABLE_PROFILES = {
    (
        "ECA concept",
        "Mathematical object",
        "Correct role",
    ): TableProfile (
        required_space = 14,
        font_command   = r"\small",
        width          = r"\textwidth",
        columns        = r"@{}p{0.20\textwidth}p{0.28\textwidth}Y@{}",
    ),
    (
        "Condition form for key $k$",
        "Mathematical representation",
        "Constraint",
    ): TableProfile (
        required_space = 8,
        font_command   = r"\small",
        width          = r"\textwidth",
        columns        = r"@{}p{0.26\textwidth}p{0.34\textwidth}Y@{}",
    ),
    (
        "Key",
        "Value domain",
    ): TableProfile (
        required_space = 8,
        font_command   = r"\small",
        width          = r"0.78\textwidth",
        columns        = r"@{}p{0.30\textwidth}Y@{}",
    ),
    (
        "Rule",
        "Event",
        "Condition set",
        r"$\sigma$",
        "Action",
    ): TableProfile (
        required_space = 17,
        font_command   = r"\footnotesize",
        width          = r"\textwidth",
        columns        = r"@{}C{0.06\textwidth}p{0.17\textwidth}Y C{0.06\textwidth}p{0.20\textwidth}@{}",
    ),
}


def parse_arguments () -> Namespace:

    # Initialize the parser by applying argument parser.

    parser = ArgumentParser (
        description = "Convert the stateless ECA technical note from Markdown to styled LaTeX.",
    )

    # Perform the add argument calls required by the parse arguments operation.

    parser.add_argument ( "--input", required = True, type = Path, help = "Input Markdown file." )
    parser.add_argument ( "--output", required = True, type = Path, help = "Output LaTeX file." )

    # Return parsed command-line arguments.

    return parser.parse_args ()


def require ( condition: bool, message: str ) -> None:

    # Reject the operation when condition is false.

    if not condition:
        raise ValueError ( message )


def read_markdown ( input_path: Path ) -> list [ str ]:

    # Perform the require and is file calls required by the read markdown operation.

    require ( input_path.is_file (), f"Markdown input does not exist: {input_path}" )

    # Initialize the text by applying read text.

    text = input_path.read_text ( encoding = "utf-8-sig" )

    # Return normalized source lines.

    return text.replace ( "\r\n", "\n" ).replace ( "\r", "\n" ).split ( "\n" )


def extract_metadata ( lines: list [ str ] ) -> tuple [ PaperMetadata, list [ str ] ]:

    line_index = 0

    # Continue processing while line index is less than the available item count and lines line index is blank.

    while line_index < len ( lines ) and not lines [ line_index ].strip ():
        line_index += 1

    # Perform the require, startswith, and len calls required by the extract metadata operation.

    require ( line_index < len ( lines ) and lines [ line_index ].startswith ( "# " ),
              "The Markdown file must begin with a level-one title." )

    # Initialize the title by applying strip.

    title = lines [ line_index ] [ 2 : ].strip ()
    line_index += 1

    # Continue processing while line index is less than the available item count and lines line index is blank.

    while line_index < len ( lines ) and not lines [ line_index ].strip ():
        line_index += 1

    # Perform the require and len calls required by the extract metadata operation.

    require ( line_index < len ( lines ), "The title must be followed by a subtitle." )

    # Initialize the subtitle by applying strip.

    subtitle = lines [ line_index ].strip ()

    # Perform the require calls required by the extract metadata operation.

    require ( subtitle, "The paper subtitle must not be empty." )
    line_index += 1

    # Continue processing while line index is less than the available item count and lines line index is blank.

    while line_index < len ( lines ) and not lines [ line_index ].strip ():
        line_index += 1

    # Initialize the author match by applying fullmatch, len, and strip.

    author_match = fullmatch ( r"\*\*(.+)\*\*", lines [ line_index ].strip () ) if line_index < len ( lines ) else None

    # Perform the require calls required by the extract metadata operation.

    require ( author_match is not None, "The subtitle must be followed by a bold author line." )

    # Initialize the author by applying strip and group.

    author = author_match.group ( 1 ).strip ()
    line_index += 1

    # Continue processing while line index is less than the available item count and lines line index is blank.

    while line_index < len ( lines ) and not lines [ line_index ].strip ():
        line_index += 1

    # Initialize the descriptor match by applying fullmatch, len, and strip.

    descriptor_match = fullmatch ( r"\*([^*]+)\*", lines [ line_index ].strip () ) if line_index < len ( lines ) else None

    # Perform the require calls required by the extract metadata operation.

    require ( descriptor_match is not None, "The author must be followed by an italic paper descriptor." )

    # Initialize the descriptor by applying strip and group.

    descriptor = descriptor_match.group ( 1 ).strip ()
    line_index += 1

    # Continue processing while line index is less than the available item count and lines line index is blank.

    while line_index < len ( lines ) and not lines [ line_index ].strip ():
        line_index += 1

    status_lines: list [ str ] = []

    # Continue processing while line index is less than the available item count and lines line index lstrip begins
    # with the expected prefix.

    while line_index < len ( lines ) and lines [ line_index ].lstrip ().startswith ( ">" ):

        # Perform the append, strip, and lstrip calls required by the extract metadata operation.

        status_lines.append ( lines [ line_index ].lstrip () [ 1 : ].strip () )
        line_index += 1

    # Perform the require calls required by the extract metadata operation.

    require ( status_lines, "The title metadata must contain a status block quote." )

    # Continue processing while line index is less than the available item count and lines line index is blank.

    while line_index < len ( lines ) and not lines [ line_index ].strip ():
        line_index += 1

    # Perform the require, len, and strip calls required by the extract metadata operation.

    require ( line_index < len ( lines ) and lines [ line_index ].strip () == "---",
              "The title metadata must end with a Markdown horizontal rule." )
    line_index += 1

    # Initialize the metadata by applying paper metadata and join.

    metadata = PaperMetadata (
        title      = title,
        subtitle   = subtitle,
        author     = author,
        descriptor = descriptor,
        status     = " ".join ( status_lines ),
    )

    # Return metadata and the remaining document lines.

    return metadata, lines [ line_index : ]


def is_table_line ( line: str ) -> bool:

    # Initialize the stripped line by applying strip.

    stripped_line = line.strip ()

    # Return whether the line is a Markdown pipe-table row.

    return stripped_line.startswith ( "|" ) and stripped_line.endswith ( "|" )


def is_table_rule ( cells: tuple [ str, ... ] ) -> bool:

    # Return whether every table cell is a Markdown separator cell.

    return bool ( cells ) and all ( TABLE_RULE_PATTERN.fullmatch ( cell.strip () ) for cell in cells )


def split_table_row ( line: str ) -> tuple [ str, ... ]:

    # Return trimmed cells from a Markdown pipe-table row.

    return tuple ( cell.strip () for cell in line.strip () [ 1 : -1 ].split ( "|" ) )


def starts_block ( line: str ) -> bool:

    # Initialize the stripped line by applying strip.

    stripped_line = line.strip ()

    # Return the branch result when stripped line is false.

    if not stripped_line:

        # Return true for this outcome.

        return True

    # Return the branch result when stripped line equals value or stripped line equals value.

    if stripped_line == "$$" or stripped_line == "---":

        # Return true for this outcome.

        return True

    # Return the branch result when the input text matches the required pattern.

    if HEADING_PATTERN.match ( stripped_line ):

        # Return true for this outcome.

        return True

    # Return the branch result when line lstrip begins with the expected prefix.

    if line.lstrip ().startswith ( ">" ):

        # Return true for this outcome.

        return True

    # Return the branch result when the is table line check succeeds.

    if is_table_line ( line ):

        # Return true for this outcome.

        return True

    # Return whether the line begins a list.

    return LIST_PATTERN.match ( line ) is not None


def parse_table ( lines: list [ str ], start_index: int ) -> tuple [ MarkdownBlock, int ]:

    table_lines: list [ str ] = []
    line_index = start_index

    # Continue processing while line index is less than the available item count and the is table line check succeeds.

    while line_index < len ( lines ) and is_table_line ( lines [ line_index ] ):

        # Perform the append calls required by the parse table operation.

        table_lines.append ( lines [ line_index ] )
        line_index += 1

    # Initialize the rows by applying tuple and split table row.

    rows = tuple ( split_table_row ( line ) for line in table_lines )

    # Perform the require, is table rule, and len calls required by the parse table operation.

    require ( len ( rows ) >= 2 and is_table_rule ( rows [ 1 ] ),
              f"Malformed Markdown table near line {start_index + 1}." )

    # Initialize the column count by applying len.

    column_count = len ( rows [ 0 ] )

    # Perform the require, all, and len calls required by the parse table operation.

    require ( column_count > 0, f"Markdown table near line {start_index + 1} has no columns." )
    require ( all ( len ( row ) == column_count for row in rows ),
              f"Markdown table near line {start_index + 1} has inconsistent columns." )

    # Initialize the block by applying markdown block.

    block = MarkdownBlock (
        kind = BLOCK_TABLE,
        rows = ( rows [ 0 ], ) + rows [ 2 : ],
    )

    # Return the table block and first unconsumed line.

    return block, line_index


def parse_list ( lines: list [ str ], start_index: int ) -> tuple [ MarkdownBlock, int ]:

    # Initialize the first match by applying match.

    first_match = LIST_PATTERN.match ( lines [ start_index ] )

    # Perform the require calls required by the parse list operation.

    require ( first_match is not None, f"Expected a list at line {start_index + 1}." )

    # Prepare the ordered and base indentation values required by the parse list operation.

    ordered          = first_match.group ( 2 ) [ 0 ].isdigit ()
    base_indentation = first_match.group ( 1 )
    items:              list [ str ] = []
    current_item_parts: list [ str ] = []
    line_index = start_index

    # Continue processing while line index is less than the available item count.

    while line_index < len ( lines ):
        line = lines [ line_index ]

        # Initialize the list match by applying match.

        list_match = LIST_PATTERN.match ( line )

        # Handle the branch where list match is not none and list match group 1 equals base indentation.

        if list_match is not None and list_match.group ( 1 ) == base_indentation:

            # Initialize the item is ordered by applying isdigit and group.

            item_is_ordered = list_match.group ( 2 ) [ 0 ].isdigit ()

            # Handle the branch where item is ordered differs from ordered.

            if item_is_ordered != ordered:
                break

            # Handle the branch where current item parts is true.

            if current_item_parts:

                # Perform the append and join calls required by the parse list operation.

                items.append ( " ".join ( current_item_parts ) )

            # Initialize the current item parts by applying strip and group.

            current_item_parts = [ list_match.group ( 3 ).strip () ]
            line_index += 1
            continue

        # Handle the branch where line contains text and the isspace check succeeds and current item parts is true.

        if line.strip () and line [ 0 ].isspace () and current_item_parts:

            # Perform the append and strip calls required by the parse list operation.

            current_item_parts.append ( line.strip () )
            line_index += 1
            continue

        break

    # Handle the branch where current item parts is true.

    if current_item_parts:

        # Perform the append and join calls required by the parse list operation.

        items.append ( " ".join ( current_item_parts ) )

    # Initialize the block by applying markdown block and tuple.

    block = MarkdownBlock (
        kind    = BLOCK_LIST,
        items   = tuple ( items ),
        ordered = ordered,
    )

    # Return the list block and first unconsumed line.

    return block, line_index


def parse_blocks ( lines: list [ str ] ) -> list [ MarkdownBlock ]:

    blocks: list [ MarkdownBlock ] = []
    line_index = 0

    # Continue processing while line index is less than the available item count.

    while line_index < len ( lines ):
        line = lines [ line_index ]

        # Initialize the stripped line by applying strip.

        stripped_line = line.strip ()

        # Handle the branch where stripped line is false or stripped line equals value.

        if not stripped_line or stripped_line == "---":
            line_index += 1
            continue

        # Initialize the heading match by applying match.

        heading_match = HEADING_PATTERN.match ( stripped_line )

        # Handle the branch where heading match is not none.

        if heading_match is not None:

            # Perform the append, markdown block, strip, len, and group calls required by the parse blocks operation.

            blocks.append (
                MarkdownBlock (
                    kind  = BLOCK_HEADING,
                    text  = heading_match.group ( 2 ).strip (),
                    level = len ( heading_match.group ( 1 ) ),
                )
            )
            line_index += 1
            continue

        # Handle the branch where stripped line equals value.

        if stripped_line == "$$":
            math_lines: list [ str ] = []
            line_index += 1

            # Continue processing while line index is less than the available item count and lines line index strip
            # differs from value.

            while line_index < len ( lines ) and lines [ line_index ].strip () != "$$":

                # Perform the append and rstrip calls required by the parse blocks operation.

                math_lines.append ( lines [ line_index ].rstrip () )
                line_index += 1

            # Perform the require, len, append, markdown block, strip, and join calls required by the parse blocks
            # operation.

            require ( line_index < len ( lines ), "Unterminated display-math block." )
            blocks.append ( MarkdownBlock ( kind = BLOCK_MATH, text = "\n".join ( math_lines ).strip () ) )
            line_index += 1
            continue

        # Handle the branch where line lstrip begins with the expected prefix.

        if line.lstrip ().startswith ( ">" ):
            quote_lines: list [ str ] = []

            # Continue processing while line index is less than the available item count and lines line index lstrip
            # begins with the expected prefix.

            while line_index < len ( lines ) and lines [ line_index ].lstrip ().startswith ( ">" ):

                # Perform the append, strip, and lstrip calls required by the parse blocks operation.

                quote_lines.append ( lines [ line_index ].lstrip () [ 1 : ].strip () )
                line_index += 1

            # Perform the append, markdown block, and join calls required by the parse blocks operation.

            blocks.append ( MarkdownBlock ( kind = BLOCK_QUOTE, text = " ".join ( quote_lines ) ) )
            continue

        # Handle the branch where the is table line check succeeds.

        if is_table_line ( line ):

            # Initialize the table block line index by applying parse table.

            table_block, line_index = parse_table ( lines, line_index )

            # Perform the append calls required by the parse blocks operation.

            blocks.append ( table_block )
            continue

        # Handle the branch where the input text matches the required pattern.

        if LIST_PATTERN.match ( line ):

            # Initialize the list block line index by applying parse list.

            list_block, line_index = parse_list ( lines, line_index )

            # Perform the append calls required by the parse blocks operation.

            blocks.append ( list_block )
            continue

        paragraph_lines: list [ str ] = []

        # Continue processing while line index is less than the available item count and the starts block check does
        # not succeed.

        while line_index < len ( lines ) and not starts_block ( lines [ line_index ] ):

            # Perform the append and strip calls required by the parse blocks operation.

            paragraph_lines.append ( lines [ line_index ].strip () )
            line_index += 1

        # Perform the require, append, markdown block, and join calls required by the parse blocks operation.

        require ( paragraph_lines, f"Unsupported Markdown block near line {line_index + 1}." )
        blocks.append ( MarkdownBlock ( kind = BLOCK_PARAGRAPH, text = " ".join ( paragraph_lines ) ) )

    # Return parsed Markdown blocks.

    return blocks


def escape_plain_text ( text: str ) -> str:

    character_replacements = {
        "\\": r"\textbackslash{}",
        "{": r"\{",
        "}": r"\}",
        "#": r"\#",
        "%": r"\%",
        "&": r"\&",
        "_": r"\_",
        "~": r"\textasciitilde{}",
        "^": r"\textasciicircum{}",
        "–": "--",
        "—": "---",
        "‑": "-",
        "“": "``",
        "”": "''",
        "‘": "`",
        "’": "'",
        "…": r"\ldots{}",
        "á": r"\'{a}",
        "Á": r"\'{A}",
        "é": r"\'{e}",
        "É": r"\'{E}",
        "í": r"\'{i}",
        "Í": r"\'{I}",
        "ó": r"\'{o}",
        "Ó": r"\'{O}",
        "ú": r"\'{u}",
        "Ú": r"\'{U}",
        "\u00a0": " ",
    }

    # Return escaped LaTeX prose.

    return "".join ( character_replacements.get ( character, character ) for character in text )


def escape_code_text ( text: str ) -> str:

    character_replacements = {
        "\\": r"\textbackslash{}",
        "{": r"\{",
        "}": r"\}",
        "#": r"\#",
        "%": r"\%",
        "&": r"\&",
        "$": r"\$",
        "_": r"\_",
        "~": r"\textasciitilde{}",
        "^": r"\textasciicircum{}",
        "–": "-",
        "—": "--",
        "‑": "-",
    }

    # Return escaped monospace text.

    return "".join ( character_replacements.get ( character, character ) for character in text )


def escape_url ( text: str ) -> str:

    character_replacements = {
        "%": r"\%",
        "#": r"\#",
        "{": r"\{",
        "}": r"\}",
        "_": r"\_",
        "&": r"\&",
    }

    # Return a URL safe for a hyperref argument.

    return "".join ( character_replacements.get ( character, character ) for character in text )


def find_closing_marker ( text: str, marker: str, start_index: int ) -> int:

    # Initialize the closing index by applying find.

    closing_index = text.find ( marker, start_index )

    # Reject the operation when closing index is less than 0.

    if closing_index < 0:
        raise ValueError ( f"Unterminated inline Markdown marker {marker!r} in: {text}" )

    # Return marker position.

    return closing_index


def find_inline_citation_numbers ( text: str ) -> list [ int ]:

    citation_numbers: list [ int ] = []
    character_index = 0

    # Continue processing while character index is less than the available item count.

    while character_index < len ( text ):

        # Handle the branch where text begins with the expected prefix.

        if text.startswith ( "**", character_index ):

            # Initialize the closing index by applying find closing marker.

            closing_index = find_closing_marker ( text, "**", character_index + 2 )
            content       = text [ character_index + 2 : closing_index ]

            # Perform the extend and find inline citation numbers calls required by the find inline citation numbers
            # operation.

            citation_numbers.extend ( find_inline_citation_numbers ( content ) )
            character_index = closing_index + 2
            continue

        # Handle the branch where text character index equals value.

        if text [ character_index ] == "*":

            # Initialize the closing index by applying find closing marker.

            closing_index = find_closing_marker ( text, "*", character_index + 1 )
            content       = text [ character_index + 1 : closing_index ]

            # Perform the extend and find inline citation numbers calls required by the find inline citation numbers
            # operation.

            citation_numbers.extend ( find_inline_citation_numbers ( content ) )
            character_index = closing_index + 1
            continue

        # Handle the branch where text character index is present in value.

        if text [ character_index ] in ( "`", "$" ):
            marker = text [ character_index ]

            # Initialize the closing index by applying find closing marker.

            closing_index   = find_closing_marker ( text, marker, character_index + 1 )
            character_index = closing_index + 1
            continue

        # Handle the branch where text character index equals value.

        if text [ character_index ] == "[":

            # Initialize the link match by applying match.

            link_match = LINK_PATTERN.match ( text, character_index )

            # Handle the branch where link match is not none.

            if link_match is not None:

                # Initialize the character index by applying end.

                character_index = link_match.end ()
                continue

            # Initialize the citation match by applying match.

            citation_match = CITATION_PATTERN.match ( text, character_index )

            # Handle the branch where citation match is not none.

            if citation_match is not None:

                # Perform the extend, int, findall, and group calls required by the find inline citation numbers
                # operation.

                citation_numbers.extend (
                    int ( number )
                    for number in CITATION_NUMBER_PATTERN.findall ( citation_match.group ( 0 ) )
                )

                # Initialize the character index by applying end.

                character_index = citation_match.end ()
                continue

        character_index += 1

    # Return citations that the inline converter will render.

    return citation_numbers


def convert_inline ( text: str ) -> str:

    output_parts: list [ str ] = []
    plain_parts:  list [ str ] = []
    character_index = 0

    def flush_plain_parts () -> None:

        # Handle the branch where plain parts is true.

        if plain_parts:

            # Perform the append, escape plain text, join, and clear calls required by the flush plain parts operation.

            output_parts.append ( escape_plain_text ( "".join ( plain_parts ) ) )
            plain_parts.clear ()

    # Continue processing while character index is less than the available item count.

    while character_index < len ( text ):

        # Handle the branch where text begins with the expected prefix.

        if text.startswith ( "**", character_index ):

            # Perform the flush plain parts calls required by the convert inline operation.

            flush_plain_parts ()

            # Initialize the closing index by applying find closing marker.

            closing_index = find_closing_marker ( text, "**", character_index + 2 )
            content       = text [ character_index + 2 : closing_index ]

            # Perform the append and convert inline calls required by the convert inline operation.

            output_parts.append ( r"\textbf{" + convert_inline ( content ) + "}" )
            character_index = closing_index + 2
            continue

        # Handle the branch where text character index equals value.

        if text [ character_index ] == "*":

            # Perform the flush plain parts calls required by the convert inline operation.

            flush_plain_parts ()

            # Initialize the closing index by applying find closing marker.

            closing_index = find_closing_marker ( text, "*", character_index + 1 )
            content       = text [ character_index + 1 : closing_index ]

            # Perform the append and convert inline calls required by the convert inline operation.

            output_parts.append ( r"\emph{" + convert_inline ( content ) + "}" )
            character_index = closing_index + 1
            continue

        # Handle the branch where text character index equals value.

        if text [ character_index ] == "`":

            # Perform the flush plain parts calls required by the convert inline operation.

            flush_plain_parts ()

            # Initialize the closing index by applying find closing marker.

            closing_index = find_closing_marker ( text, "`", character_index + 1 )
            content       = text [ character_index + 1 : closing_index ]

            # Perform the append and escape code text calls required by the convert inline operation.

            output_parts.append ( r"\texttt{" + escape_code_text ( content ) + "}" )
            character_index = closing_index + 1
            continue

        # Handle the branch where text character index equals value.

        if text [ character_index ] == "$":

            # Perform the flush plain parts calls required by the convert inline operation.

            flush_plain_parts ()

            # Initialize the closing index by applying find closing marker.

            closing_index = find_closing_marker ( text, "$", character_index + 1 )
            content       = text [ character_index + 1 : closing_index ]

            # Perform the append calls required by the convert inline operation.

            output_parts.append ( "$" + content + "$" )
            character_index = closing_index + 1
            continue

        # Handle the branch where text character index equals value.

        if text [ character_index ] == "[":

            # Initialize the link match by applying match.

            link_match = LINK_PATTERN.match ( text, character_index )

            # Handle the branch where link match is not none.

            if link_match is not None:

                # Perform the flush plain parts calls required by the convert inline operation.

                flush_plain_parts ()

                # Prepare the link text and link URL values required by the convert inline operation.

                link_text = convert_inline ( link_match.group ( 1 ) )
                link_url  = escape_url ( link_match.group ( 2 ) )

                # Perform the append calls required by the convert inline operation.

                output_parts.append ( rf"\href{{{link_url}}}{{{link_text}}}" )

                # Initialize the character index by applying end.

                character_index = link_match.end ()
                continue

            # Initialize the citation match by applying match.

            citation_match = CITATION_PATTERN.match ( text, character_index )

            # Handle the branch where citation match is not none.

            if citation_match is not None:

                # Perform the flush plain parts calls required by the convert inline operation.

                flush_plain_parts ()

                # Prepare the citation numbers and citation keys values required by the convert inline operation.

                citation_numbers = CITATION_NUMBER_PATTERN.findall ( citation_match.group ( 0 ) )
                citation_keys    = ",".join ( f"reference-{number}" for number in citation_numbers )

                # Perform the append calls required by the convert inline operation.

                output_parts.append ( rf"\cite{{{citation_keys}}}" )

                # Initialize the character index by applying end.

                character_index = citation_match.end ()
                continue

        # Perform the append calls required by the convert inline operation.

        plain_parts.append ( text [ character_index ] )
        character_index += 1

    # Perform the flush plain parts calls required by the convert inline operation.

    flush_plain_parts ()

    # Return converted inline Markdown.

    return "".join ( output_parts )


def wrap_latex ( text: str, initial_indent: str = "", subsequent_indent: str = "" ) -> list [ str ]:

    # Initialize the wrapper by applying text wrapper.

    wrapper = TextWrapper (
        width              = PARAGRAPH_WRAPPER.width,
        initial_indent     = initial_indent,
        subsequent_indent  = subsequent_indent,
        break_long_words   = False,
        break_on_hyphens   = False,
        replace_whitespace = False,
    )

    # Return wrapped LaTeX source lines.

    return wrapper.fill ( text ).splitlines ()


def normalized_heading ( heading: str ) -> str:

    # Return a heading without manually authored section numbering.

    return NUMBER_PREFIX.sub ( "", heading ).strip ()


def math_lines ( content: str ) -> list [ str ]:

    output_lines = [ r"\begin{equation}" ]

    # Perform the extend, splitlines, and append calls required by the math lines operation.

    output_lines.extend ( f"    {line}" if line else "" for line in content.splitlines () )
    output_lines.append ( r"\end{equation}" )

    # Return one numbered display equation.

    return output_lines


class LatexRenderer:

    def __init__ ( self, metadata: PaperMetadata, blocks: list [ MarkdownBlock ] ):

        self.metadata = metadata
        self.blocks   = blocks

        # Perform the validate citations calls required by the init operation.

        self.validate_citations ()

    def validate_citations ( self ) -> None:

        reference_count = 0

        # Process each block index block supplied by enumerate blocks.

        for block_index, block in enumerate ( self.blocks ):

            # Handle the branch where block kind equals block heading and block level equals 2 and normalized heading
            # block text casefold equals references and block index 1 is less than the available item count and blocks
            # block index 1 kind equals block list and blocks block index 1 ordered is true.

            if (
                block.kind == BLOCK_HEADING
                and block.level == 2
                and normalized_heading ( block.text ).casefold () == "references"
                and block_index + 1 < len ( self.blocks )
                and self.blocks [ block_index + 1 ].kind == BLOCK_LIST
                and self.blocks [ block_index + 1 ].ordered
            ):

                # Initialize the reference count by applying len.

                reference_count = len ( self.blocks [ block_index + 1 ].items )
                break

        # Perform the require calls required by the validate citations operation.

        require ( reference_count > 0, "The Markdown document must contain a numbered References list." )

        inline_texts: list [ str ] = [
            self.metadata.subtitle,
            self.metadata.descriptor,
            self.metadata.status,
        ]

        # Process each block supplied by blocks.

        for block in self.blocks:

            # Handle the branch where block kind is present in block heading block paragraph block quote.

            if block.kind in ( BLOCK_HEADING, BLOCK_PARAGRAPH, BLOCK_QUOTE ):

                # Perform the append calls required by the validate citations operation.

                inline_texts.append ( block.text )

            # Handle the alternative where block kind equals block list.

            elif block.kind == BLOCK_LIST:

                # Perform the extend calls required by the validate citations operation.

                inline_texts.extend ( block.items )

            # Handle the alternative where block kind equals block table.

            elif block.kind == BLOCK_TABLE:

                # Perform the extend calls required by the validate citations operation.

                inline_texts.extend ( cell for row in block.rows for cell in row )

        # Initialize the invalid citations by applying set.

        invalid_citations: set [ int ] = set ()

        # Process each inline text supplied by inline texts.

        for inline_text in inline_texts:

            # Perform the update and find inline citation numbers calls required by the validate citations operation.

            invalid_citations.update (
                citation_number
                for citation_number in find_inline_citation_numbers ( inline_text )
                if citation_number < 1 or citation_number > reference_count
            )

        # Perform the require, join, and sorted calls required by the validate citations operation.

        require (
            not invalid_citations,
            "Citation number(s) have no corresponding reference: "
            + ", ".join ( f"[{number}]" for number in sorted ( invalid_citations ) ),
        )

    def render ( self ) -> str:

        # Prepare the abstract blocks body blocks and abstract text keywords text values required by the render
        # operation.

        abstract_blocks, body_blocks = self.split_abstract ()
        abstract_text, keywords_text = self.render_abstract_content ( abstract_blocks )

        preamble = DOCUMENT_PREAMBLE

        # Prepare the preamble and output lines values required by the render operation.

        preamble = preamble.replace ( "<<PDF_TITLE>>", escape_plain_text ( self.metadata.title.replace ( "–", "-" ) ) )
        preamble = preamble.replace ( "<<PDF_AUTHOR>>", escape_plain_text ( self.metadata.author ) )
        preamble = preamble.replace ( "<<PDF_KEYWORDS>>", escape_plain_text ( keywords_text.replace ( "–", "-" ) ) )

        output_lines = preamble.rstrip ().splitlines ()

        # Perform the extend, render title, wrap latex, convert inline, append, render blocks, and rstrip calls
        # required by the render operation.

        output_lines.extend ( [ "", *self.render_title (), "" ] )
        output_lines.extend ( [ r"\begin{abstract}", *abstract_text, r"\end{abstract}", "" ] )
        output_lines.extend (
            wrap_latex (
                convert_inline ( f"**Keywords:** {keywords_text}" ),
                initial_indent = r"\noindent ",
            )
        )
        output_lines.append ( "" )
        output_lines.extend ( self.render_blocks ( body_blocks ) )
        output_lines.append ( DOCUMENT_END.rstrip () )

        # Prepare the latex and non ascii characters values required by the render operation.

        latex                = "\n".join ( output_lines ).rstrip () + "\n"
        non_ascii_characters = sorted ( { character for character in latex if ord ( character ) > 127 } )

        # Perform the require, join, and ord calls required by the render operation.

        require (
            not non_ascii_characters,
            "Generated LaTeX contains unsupported non-ASCII characters: "
            + ", ".join ( f"U+{ord ( character ):04X}" for character in non_ascii_characters ),
        )

        # Return the complete LaTeX document.

        return latex

    def split_abstract ( self ) -> tuple [ list [ MarkdownBlock ], list [ MarkdownBlock ] ]:

        # Initialize the abstract heading index by applying next, enumerate, casefold, and normalized heading.

        abstract_heading_index = next (
            (
                index
                for index, block in enumerate ( self.blocks )
                if block.kind == BLOCK_HEADING
                and block.level == 2
                and normalized_heading ( block.text ).casefold () == "abstract"
            ),
            -1,
        )

        # Perform the require calls required by the split abstract operation.

        require ( abstract_heading_index >= 0, "The Markdown document must contain an Abstract section." )

        # Initialize the body start index by applying next, range, and len.

        body_start_index = next (
            (
                index
                for index in range ( abstract_heading_index + 1, len ( self.blocks ) )
                if self.blocks [ index ].kind == BLOCK_HEADING and self.blocks [ index ].level == 2
            ),
            -1,
        )

        # Perform the require calls required by the split abstract operation.

        require ( body_start_index >= 0, "The Abstract must be followed by a numbered section." )

        # Return abstract blocks and remaining body blocks.

        return self.blocks [ abstract_heading_index + 1 : body_start_index ], self.blocks [ body_start_index : ]

    def render_abstract_content ( self, blocks: list [ MarkdownBlock ] ) -> tuple [ list [ str ], str ]:

        output_lines: list [ str ] = []
        keywords_text = ""

        # Process each block supplied by blocks.

        for block in blocks:

            # Perform the require calls required by the render abstract content operation.

            require ( block.kind == BLOCK_PARAGRAPH, "The Abstract may contain paragraphs only." )

            # Handle the branch where block text begins with the expected prefix.

            if block.text.startswith ( "**Keywords:**" ):

                # Initialize the keywords text by applying strip and len.

                keywords_text = block.text [ len ( "**Keywords:**" ) : ].strip ()
                continue

            # Perform the extend, wrap latex, convert inline, and append calls required by the render abstract content
            # operation.

            output_lines.extend ( wrap_latex ( convert_inline ( block.text ) ) )
            output_lines.append ( "" )

        # Perform the require calls required by the render abstract content operation.

        require ( keywords_text, "The Abstract must end with a bold Keywords paragraph." )

        # Handle the branch where output lines is true and output lines 1 is false.

        if output_lines and not output_lines [ -1 ]:

            # Perform the pop calls required by the render abstract content operation.

            output_lines.pop ()

        # Return abstract source and plain keyword text.

        return output_lines, keywords_text

    def render_title ( self ) -> list [ str ]:

        # Prepare the title and title lines values required by the render title operation.

        title = self.metadata.title.replace ( "–", "-" ).replace ( "—", "-" )
        title_lines = TextWrapper (
            width            = 58,
            break_long_words = False,
            break_on_hyphens = False,
        ).wrap ( title )

        output_lines = [ r"\begin{center}" ]

        # Perform the extend, escape plain text, wrap latex, and convert inline calls required by the render title
        # operation.

        output_lines.extend (
            rf"    {{\LARGE\bfseries {escape_plain_text ( title_line )}\par}}"
            for title_line in title_lines
        )
        output_lines.extend (
            [
                r"    \vspace{0.45em}",
                rf"    {{\large\itshape {convert_inline ( self.metadata.subtitle )}\par}}",
                r"    \vspace{0.9em}",
                rf"    {{\large {escape_plain_text ( self.metadata.author )}, {PAPER_YEAR}\par}}",
                r"    \vspace{0.25em}",
                rf"    {{\normalsize\itshape {convert_inline ( self.metadata.descriptor )}\par}}",
                r"\end{center}",
                "",
                r"\vspace{1.1em}",
                r"\noindent\rule{\textwidth}{0.4pt}",
                "",
                r"\begin{quote}",
                r"\itshape",
                *wrap_latex ( convert_inline ( self.metadata.status ) ),
                r"\end{quote}",
                "",
                r"\noindent\rule{\textwidth}{0.4pt}",
                r"\vspace{0.7em}",
            ]
        )

        # Return title matter.

        return output_lines

    def render_blocks ( self, blocks: list [ MarkdownBlock ] ) -> list [ str ]:

        output_lines: list [ str ] = []
        block_index        = 0
        current_subsection = ""

        # Continue processing while block index is less than the available item count.

        while block_index < len ( blocks ):
            block = blocks [ block_index ]

            # Initialize the compound output consumed block count by applying render compound block.

            compound_output, consumed_block_count = self.render_compound_block (
                blocks,
                block_index,
                current_subsection,
            )

            # Handle the branch where consumed block count is true.

            if consumed_block_count:

                # Perform the extend and append calls required by the render blocks operation.

                output_lines.extend ( compound_output )
                output_lines.append ( "" )
                block_index += consumed_block_count
                continue

            # Handle the branch where block kind equals block heading.

            if block.kind == BLOCK_HEADING:

                # Initialize the heading by applying normalized heading.

                heading = normalized_heading ( block.text )

                # Handle the branch where heading casefold equals references.

                if heading.casefold () == "references":

                    # Initialize the bibliography output consumed block count by applying render bibliography.

                    bibliography_output, consumed_block_count = self.render_bibliography ( blocks, block_index )

                    # Perform the extend and append calls required by the render blocks operation.

                    output_lines.extend ( bibliography_output )
                    output_lines.append ( "" )
                    block_index += consumed_block_count
                    continue

                heading_commands = {
                    2: "section",
                    3: "subsection",
                    4: "subsubsection",
                    5: "paragraph",
                    6: "subparagraph",
                }
                command = heading_commands [ block.level ]

                # Perform the append and convert inline calls required by the render blocks operation.

                output_lines.append ( rf"\{command}{{{convert_inline ( heading )}}}" )

                # Handle the branch where block level equals 2.

                if block.level == 2:
                    current_subsection = ""

                # Handle the alternative where block level equals 3.

                elif block.level == 3:
                    current_subsection = heading

            # Handle the alternative where block kind equals block paragraph.

            elif block.kind == BLOCK_PARAGRAPH:

                # Perform the extend, wrap latex, and convert inline calls required by the render blocks operation.

                output_lines.extend ( wrap_latex ( convert_inline ( block.text ) ) )

            # Handle the alternative where block kind equals block math.

            elif block.kind == BLOCK_MATH:

                # Perform the extend and math lines calls required by the render blocks operation.

                output_lines.extend ( math_lines ( block.text ) )

            # Handle the alternative where block kind equals block quote.

            elif block.kind == BLOCK_QUOTE:

                # Perform the extend, wrap latex, and convert inline calls required by the render blocks operation.

                output_lines.extend (
                    [
                        r"\begin{quote}",
                        *wrap_latex ( convert_inline ( block.text ) ),
                        r"\end{quote}",
                    ]
                )

            # Handle the alternative where block kind equals block list.

            elif block.kind == BLOCK_LIST:

                # Perform the extend and render list calls required by the render blocks operation.

                output_lines.extend ( self.render_list ( block ) )

            # Handle the alternative where block kind equals block table.

            elif block.kind == BLOCK_TABLE:

                # Perform the extend and render table calls required by the render blocks operation.

                output_lines.extend ( self.render_table ( block ) )

            # Handle the alternative path when the preceding condition is false.

            else:
                raise ValueError ( f"Unsupported block kind: {block.kind}" )

            # Perform the append calls required by the render blocks operation.

            output_lines.append ( "" )
            block_index += 1

        # Continue processing while output lines is true and output lines 1 is false.

        while output_lines and not output_lines [ -1 ]:

            # Perform the pop calls required by the render blocks operation.

            output_lines.pop ()

        # Return rendered body blocks.

        return output_lines

    def render_compound_block ( self, blocks: list [ MarkdownBlock ], block_index: int,
                                current_subsection: str ) -> tuple [ list [ str ], int ]:

        block = blocks [ block_index ]

        # Handle the branch where block kind equals block paragraph and block text equals let and block index 3 is less
        # than the available item count and blocks block index 1 kind equals block math and blocks block index 2 kind
        # equals block paragraph and blocks block index 2 text equals also let and blocks block index 3 kind equals
        # block math.

        if (
            block.kind == BLOCK_PARAGRAPH
            and block.text == "Let"
            and block_index + 3 < len ( blocks )
            and blocks [ block_index + 1 ].kind == BLOCK_MATH
            and blocks [ block_index + 2 ].kind == BLOCK_PARAGRAPH
            and blocks [ block_index + 2 ].text == "Also let"
            and blocks [ block_index + 3 ].kind == BLOCK_MATH
        ):

            # Prepare the first math and second math values required by the render compound block operation.

            first_math    = blocks [ block_index + 1 ].text.rstrip ().removesuffix ( "." )
            second_math   = blocks [ block_index + 3 ].text.rstrip ()
            combined_math = first_math + ",\n\\qquad\n" + second_math

            # Return combined Boolean and natural-number definitions.

            return [ "Let", *math_lines ( combined_math ) ], 4

        # Handle the branch where block kind equals block paragraph and block text begins with the expected prefix and
        # block index 3 is less than the available item count and blocks block index 1 kind equals block math and
        # blocks block index 2 kind equals block paragraph and blocks block index 2 text begins with the expected
        # prefix and blocks block index 3 kind equals block math.

        if (
            block.kind == BLOCK_PARAGRAPH
            and block.text.startswith ( "An event type " )
            and block_index + 3 < len ( blocks )
            and blocks [ block_index + 1 ].kind == BLOCK_MATH
            and blocks [ block_index + 2 ].kind == BLOCK_PARAGRAPH
            and blocks [ block_index + 2 ].text.startswith ( "The set of event occurrences is" )
            and blocks [ block_index + 3 ].kind == BLOCK_MATH
        ):

            # Prepare the occurrence notation and introductory text values required by the render compound block
            # operation.

            occurrence_notation = blocks [ block_index + 1 ].text.rstrip ().removesuffix ( "." )
            introductory_text   = block.text.rstrip ().removesuffix ( ":" )
            introductory_text += f", written ${occurrence_notation}$."

            # Initialize the output lines by applying wrap latex, math lines, and convert inline.

            output_lines = [
                *wrap_latex ( convert_inline ( introductory_text ) ),
                "",
                r"\begin{definition}[Event occurrence]",
                *wrap_latex ( convert_inline ( blocks [ block_index + 2 ].text ) ),
                *math_lines ( blocks [ block_index + 3 ].text ),
                r"\end{definition}",
            ]

            # Return the event-occurrence definition.

            return output_lines, 4

        definition_title = ""

        # Handle the branch where block kind equals block paragraph and block text begins with the expected prefix.

        if block.kind == BLOCK_PARAGRAPH and block.text.startswith ( "A payload for " ):
            definition_title = "Payload"

        # Handle the alternative where block kind equals block paragraph and block text begins with the expected
        # prefix.

        elif block.kind == BLOCK_PARAGRAPH and block.text.startswith ( "A condition set is " ):
            definition_title = "Condition set"

        # Handle the alternative where block kind equals block paragraph and block text begins with the expected
        # prefix.

        elif block.kind == BLOCK_PARAGRAPH and block.text.startswith ( "The specificity of " ):
            definition_title = "Specificity"

        # Handle the branch where definition title is true and block index 1 is less than the available item count and
        # blocks block index 1 kind equals block math.

        if (
            definition_title
            and block_index + 1 < len ( blocks )
            and blocks [ block_index + 1 ].kind == BLOCK_MATH
        ):

            # Initialize the output lines by applying wrap latex, math lines, and convert inline.

            output_lines = [
                rf"\begin{{definition}}[{definition_title}]",
                *wrap_latex ( convert_inline ( block.text ) ),
                *math_lines ( blocks [ block_index + 1 ].text ),
                r"\end{definition}",
            ]

            # Return a paragraph-and-equation definition.

            return output_lines, 2

        # Handle the branch where block kind equals block paragraph and a rule is a tuple is present in block text and
        # block index 3 is less than the available item count and blocks block index 1 kind equals block math and
        # blocks block index 2 kind equals block paragraph and blocks block index 2 text equals where and blocks block
        # index 3 kind equals block list.

        if (
            block.kind == BLOCK_PARAGRAPH
            and "A rule is a tuple" in block.text
            and block_index + 3 < len ( blocks )
            and blocks [ block_index + 1 ].kind == BLOCK_MATH
            and blocks [ block_index + 2 ].kind == BLOCK_PARAGRAPH
            and blocks [ block_index + 2 ].text == "where"
            and blocks [ block_index + 3 ].kind == BLOCK_LIST
        ):

            # Initialize the output lines by applying wrap latex, math lines, render list, and convert inline.

            output_lines = [
                r"\begin{definition}[Rule]",
                *wrap_latex ( convert_inline ( block.text ) ),
                *math_lines ( blocks [ block_index + 1 ].text ),
                *wrap_latex ( convert_inline ( blocks [ block_index + 2 ].text ) ),
                *self.render_list ( blocks [ block_index + 3 ] ),
                r"\end{definition}",
            ]

            # Return the rule definition.

            return output_lines, 4

        # Handle the branch where block kind equals block paragraph and define the rule query is present in block text
        # and block index 3 is less than the available item count and blocks block index 1 kind equals block math and
        # blocks block index 2 kind equals block paragraph and blocks block index 2 text equals by and blocks block
        # index 3 kind equals block math.

        if (
            block.kind == BLOCK_PARAGRAPH
            and "Define the rule query" in block.text
            and block_index + 3 < len ( blocks )
            and blocks [ block_index + 1 ].kind == BLOCK_MATH
            and blocks [ block_index + 2 ].kind == BLOCK_PARAGRAPH
            and blocks [ block_index + 2 ].text == "by"
            and blocks [ block_index + 3 ].kind == BLOCK_MATH
        ):

            # Initialize the output lines by applying wrap latex, math lines, and convert inline.

            output_lines = [
                r"\begin{definition}[Stateless ECA rule query]",
                *wrap_latex ( convert_inline ( block.text ) ),
                *math_lines ( blocks [ block_index + 1 ].text ),
                *wrap_latex ( convert_inline ( blocks [ block_index + 2 ].text ) ),
                *math_lines ( blocks [ block_index + 3 ].text ),
                r"\end{definition}",
            ]

            # Return the option-valued query definition.

            return output_lines, 4

        # Handle the branch where block kind equals block paragraph and block text begins with the expected prefix.

        if block.kind == BLOCK_PARAGRAPH and block.text.startswith ( "**Proposition.**" ):

            # Initialize the proposition text by applying strip and len.

            proposition_text  = block.text [ len ( "**Proposition.**" ) : ].strip ()
            proposition_start = r"\begin{proposition}"

            # Handle the branch where current subsection is true.

            if current_subsection:

                # Initialize the proposition start by applying convert inline.

                proposition_start = rf"\begin{{proposition}}[{convert_inline ( current_subsection )}]"

            # Return a formal proposition.

            return [
                proposition_start,
                *wrap_latex ( convert_inline ( proposition_text ) ),
                r"\end{proposition}",
            ], 1

        # Handle the branch where block kind equals block paragraph and block text begins with the expected prefix.

        if block.kind == BLOCK_PARAGRAPH and block.text.startswith ( "**Reason.**" ):

            # Initialize the proof text by applying strip and len.

            proof_text = block.text [ len ( "**Reason.**" ) : ].strip ()

            # Return a formal proof.

            return [
                r"\begin{proof}",
                *wrap_latex ( convert_inline ( proof_text ) ),
                r"\end{proof}",
            ], 1

        # Return no special rendering.

        return [], 0

    def render_list ( self, block: MarkdownBlock ) -> list [ str ]:

        environment  = "enumerate" if block.ordered else "itemize"
        output_lines = [ rf"\begin{{{environment}}}" ]

        # Process each item supplied by block items.

        for item in block.items:

            # Perform the extend, wrap latex, and convert inline calls required by the render list operation.

            output_lines.extend (
                wrap_latex (
                    convert_inline ( item ),
                    initial_indent    = r"    \item ",
                    subsequent_indent = "          ",
                )
            )

        # Perform the append calls required by the render list operation.

        output_lines.append ( rf"\end{{{environment}}}" )

        # Return a LaTeX list.

        return output_lines

    def render_table ( self, block: MarkdownBlock ) -> list [ str ]:

        # Perform the require calls required by the render table operation.

        require ( block.rows, "Cannot render an empty table." )

        header = block.rows [ 0 ]

        # Initialize the profile by applying get.

        profile = TABLE_PROFILES.get ( header )

        # Handle the branch where profile is none.

        if profile is None:

            # Prepare the column count and profile values required by the render table operation.

            column_count = len ( header )
            profile = TableProfile (
                required_space = 8,
                font_command   = r"\small",
                width          = r"\textwidth",
                columns        = "@{}" + " ".join ( "Y" for _ in range ( column_count ) ) + "@{}",
            )

        # Prepare the header cells and output lines values required by the render table operation.

        header_cells = [ rf"\textbf{{{convert_inline ( cell )}}}" for cell in header ]
        output_lines = [
            rf"\Needspace{{{profile.required_space}\baselineskip}}",
            r"\begin{center}",
            profile.font_command,
            rf"\begin{{tabularx}}{{{profile.width}}}{{{profile.columns}}}",
            r"\toprule",
            " & ".join ( header_cells ) + r" \\",
            r"\midrule",
        ]

        # Process each row supplied by block rows 1.

        for row in block.rows [ 1 : ]:

            # Perform the append, join, and convert inline calls required by the render table operation.

            output_lines.append ( " & ".join ( convert_inline ( cell ) for cell in row ) + r" \\" )

        # Perform the extend calls required by the render table operation.

        output_lines.extend (
            [
                r"\bottomrule",
                r"\end{tabularx}",
                r"\end{center}",
            ]
        )

        # Return a styled LaTeX table.

        return output_lines

    def render_bibliography ( self, blocks: list [ MarkdownBlock ],
                              block_index: int ) -> tuple [ list [ str ], int ]:

        # Perform the require and len calls required by the render bibliography operation.

        require (
            block_index + 1 < len ( blocks )
            and blocks [ block_index + 1 ].kind == BLOCK_LIST
            and blocks [ block_index + 1 ].ordered,
            "The References heading must be followed by a numbered list.",
        )

        reference_list = blocks [ block_index + 1 ]
        output_lines   = [ r"\clearpage", r"\begin{thebibliography}{99}", "" ]

        # Process each reference number reference supplied by enumerate reference list items start 1.

        for reference_number, reference in enumerate ( reference_list.items, start = 1 ):

            # Perform the append, extend, wrap latex, and convert inline calls required by the render bibliography
            # operation.

            output_lines.append ( rf"\bibitem{{reference-{reference_number}}}" )
            output_lines.extend ( wrap_latex ( convert_inline ( reference ) ) )
            output_lines.append ( "" )

        # Perform the append calls required by the render bibliography operation.

        output_lines.append ( r"\end{thebibliography}" )

        # Return the bibliography and consumed heading/list count.

        return output_lines, 2


def write_latex ( output_path: Path, latex: str ) -> None:

    # Perform the mkdir calls required by the write latex operation.

    output_path.parent.mkdir ( parents = True, exist_ok = True )

    # Initialize the temporary path by applying with suffix.

    temporary_path = output_path.with_suffix ( output_path.suffix + ".tmp" )

    # Perform the write text and replace calls required by the write latex operation.

    temporary_path.write_text ( latex, encoding = "ascii", newline = "\n" )
    temporary_path.replace ( output_path )


def main () -> int:

    # Prepare the arguments, lines, metadata body lines, blocks, and latex values required by the main operation.

    arguments            = parse_arguments ()
    lines                = read_markdown ( arguments.input )
    metadata, body_lines = extract_metadata ( lines )
    blocks               = parse_blocks ( body_lines )
    latex                = LatexRenderer ( metadata, blocks ).render ()

    # Perform the write latex and print calls required by the main operation.

    write_latex ( arguments.output, latex )
    print ( f"Generated LaTeX: {arguments.output}" )

    # Return success.

    return 0

# Reject the operation when name equals main.

if __name__ == "__main__":
    raise SystemExit ( main () )

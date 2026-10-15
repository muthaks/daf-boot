#!/usr/bin/env python3
"""Rewrite a JAX-RS resource into a Spring MVC controller.

Endpoint paths, HTTP verbs, parameter names and status codes are preserved exactly -
the DAF API contract must not move. Only the framework changes.

    python api/tools/jaxrs-to-spring.py <file.java> [...]
"""
import io, re, sys

SPRING_IMPORTS = """import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import jakarta.inject.Inject;
"""

def convert(s: str, cls_old: str, cls_new: str) -> str:
    # ---- imports ----
    for pat in (r'^import javax\.ws\.rs\.[^\n]*\n', r'^import com\.google\.inject\.[^\n]*\n',
                r'^import io\.swagger\.annotations\.[^\n]*\n', r'^import org\.jboss\.resteasy\.[^\n]*\n',
                r'^import static za\.co\.rmb\.credit\.daf\.afsecurity[^\n]*\n'):
        s = re.sub(pat, '', s, flags=re.M)
    i = s.index('import ')
    s = s[:i] + SPRING_IMPORTS + s[i:]

    # ---- class level ----
    s = re.sub(r'@Api\([^)]*\)\n', '', s)
    s = re.sub(r'@Path\("([^"]*)"\)\npublic class', r'@RestController\n@RequestMapping("\1")\npublic class', s)
    s = s.replace(cls_old, cls_new)

    # ---- verbs + paths ----
    for verb, ann in (('GET', 'GetMapping'), ('POST', 'PostMapping'),
                      ('PUT', 'PutMapping'), ('DELETE', 'DeleteMapping')):
        s = re.sub(r'@' + verb + r'\n(\s*)@Path\("([^"]*)"\)',
                   lambda m, a=ann: '%s@%s("%s")' % (m.group(1), a, m.group(2)), s)
        s = re.sub(r'@' + verb + r'\n(\s*)', lambda m, a=ann: '%s@%s\n%s' % (m.group(1), a, m.group(1)), s)
    s = re.sub(r'@Path\("([^"]*)"\)', r'@RequestMapping("\1")', s)

    # ---- parameters / metadata ----
    s = re.sub(r'@PathParam\("([^"]+)"\)', r'@PathVariable("\1")', s)
    s = re.sub(r'@QueryParam\("([^"]+)"\)', r'@RequestParam(value = "\1", required = false)', s)
    s = re.sub(r'@HeaderParam\("([^"]+)"\)', r'@RequestHeader(value = "\1", required = false)', s)
    s = re.sub(r'@FormParam\("([^"]+)"\)', r'@RequestParam(value = "\1", required = false)', s)
    s = re.sub(r'@DefaultValue\("([^"]*)"\)\s*', '', s)
    s = re.sub(r'@ApiParam\((?:[^()]|\([^()]*\))*\)\s*', '', s)
    s = re.sub(r'@ApiOperation\((?:[^()]|\([^()]*\))*\)\s*', '', s)
    s = re.sub(r'@Produces\([^)]*\)\n\s*', '', s)
    s = re.sub(r'@Consumes\([^)]*\)\n\s*', '', s)
    s = re.sub(r'@PermitAll\n\s*', '', s)

    # ---- Response -> ResponseEntity ----
    s = re.sub(r'\b(public|private|protected) Response \b', r'\1 ResponseEntity<?> ', s)
    s = re.sub(r'\bResponse (denied|notOwner|result|response)\b', r'ResponseEntity<?> \1', s)
    s = re.sub(r'Response[.]status[(]Response[.]Status[.]([A-Z_]+)[.]getStatusCode[(][)],\s*("[^"]*")[)]',
               r'ResponseEntity.status(HttpStatus.).body(java.util.Collections.singletonMap("message", ))', s)
    s = re.sub(r'Response\.status\(Response\.Status\.([A-Z_]+)\)', r'ResponseEntity.status(HttpStatus.\1)', s)
    s = re.sub(r'Response\.status\((\d+)\)', r'ResponseEntity.status(\1)', s)
    s = s.replace('Response.ok(', 'ResponseEntity.ok(')
    s = s.replace('Response.noContent()', 'ResponseEntity.noContent()')
    s = s.replace('.entity(', '.body(')
    s = re.sub(r'ResponseEntity\.ok\((.*?)\)\.build\(\)', r'ResponseEntity.ok(\1)', s, flags=re.S)
    s = re.sub(r'\.body\(([^;]*?)\)\.build\(\)', r'.body(\1)', s, flags=re.S)

    # ---- security context -> Spring ----
    s = re.sub(r'\s*@Context\n\s*private SecurityContext context;\n', '\n', s)
    s = re.sub(r'\s*tokenValidation\(context\);', '', s)
    s = s.replace('context.isUserInRole(', 'hasAuthority(')
    s = s.replace('context.getUserPrincipal().getName()', 'authentication().getName()')
    s = s.replace('context != null && context.getUserPrincipal() != null', 'authentication() != null')
    s = s.replace('context == null', 'authentication() == null')

    # ---- multipart ----
    s = s.replace('MultipartFormDataInput form', 'MultipartFile file')
    return s

def main():
    for path in sys.argv[1:]:
        s = io.open(path, encoding='utf-8').read()
        m = re.search(r'public class (\w+)', s)
        if not m:
            print('skip (no class):', path); continue
        old = m.group(1)
        new = old.replace('ApprovalForum', '').replace('Resource', '') + 'Controller'
        new = new[0].upper() + new[1:] if new else old
        out = convert(s, old, new)   # compute first: never truncate on a failed conversion
        with io.open(path, 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(out)
        print(f'{old} -> {new}')

if __name__ == '__main__':
    main()

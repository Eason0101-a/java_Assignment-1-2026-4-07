$utf8 = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8
chcp 65001 > $null

javac SemiAutoHeightEstimator.java
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 SemiAutoHeightEstimator

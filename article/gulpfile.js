const gulp = require('gulp');
const clean = require('gulp-clean');
const concat = require('gulp-concat');
const markdownPdf = require('gulp-markdown-pdf');
const path = require('path');
const fs = require('fs');
const { execSync } = require('child_process');

const info = require('./package.json');
const files = require('./src/index.json');

gulp.task('pdf', function() {
    const sources = files.map(f => './src/' + f);

    gulp.src(sources)
        .pipe(concat(info.name + '.md'))
        .pipe(gulp.dest('dist'))
        .pipe(markdownPdf())
        .pipe(gulp.dest('dist'));
});

gulp.task('odt', function() {
    const src = path.resolve(__dirname, 'src');
    let cmd = 'docker run -u $(id -u) --rm  -v ' + src + ':/data -w /data cloudogu/pandoc:0.7.0 '
    for (let file of files) {
        cmd += ' ' + file
    }
    cmd += ' -o /data/document.odt'
    execSync(cmd)

    const srcDoc = path.resolve(src, 'document.odt');

    const dist = path.resolve(__dirname, 'dist');
    if (!fs.existsSync(dist)) {
        fs.mkdirSync(dist);
    }
    const distDoc = path.resolve(dist, info.name + '.odt');
    fs.renameSync(srcDoc, distDoc);
});

gulp.task('cloudogu-pdf', function() {
    const src = path.resolve(__dirname, 'src');
    let cmd = 'docker run -u $(id -u) --rm  -v ' + src + ':/data cloudogu/doc_template:0.15.0'
    for (let file of files) {
        cmd += ' ' + file;
    }
    execSync(cmd)

    const srcDoc = path.resolve(src, 'document.pdf');

    const dist = path.resolve(__dirname, 'dist');
    if (!fs.existsSync(dist)) {
        fs.mkdirSync(dist);
    }
    const distDoc = path.resolve(dist, info.name + '.pdf');
    fs.renameSync(srcDoc, distDoc);
});

gulp.task('clean', function(){
    gulp.src('./dist', {read: false})
        .pipe(clean());
});

gulp.task('default', ['pdf']);
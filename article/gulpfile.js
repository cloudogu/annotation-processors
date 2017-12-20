const gulp = require('gulp');
const clean = require('gulp-clean');
const concat = require('gulp-concat');
const markdownPdf = require('gulp-markdown-pdf');

gulp.task('pdf', function() {
    const info = require('./package.json');
    const files = require('./src/index.json');

    const sources = files.map(f => './src/' + f);

    gulp.src(sources)
        .pipe(concat(info.name + '.md'))
        .pipe(gulp.dest('dist'))
        .pipe(markdownPdf())
        .pipe(gulp.dest('dist'));
});

gulp.task('clean', function(){
    gulp.src('./dist', {read: false})
        .pipe(clean());
});
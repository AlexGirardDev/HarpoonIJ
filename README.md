<div align="center">

# HarpoonIJ

HarpoonIJ is a port of the NeoVim Extension [Harpoon](https://github.com/ThePrimeagen/harpoon) created by [ThePrimeagen](https://twitter.com/ThePrimeagen)
</div>

## Features 

Lets you set up to 5 files that you can have direct hotkeys to jump too.
Has popup dialog to display your current selected files
Can Directly set file to specific index or just add file to first empty spot

![Navigation Example](images/navigation.gif)


### Commands

`ShowHarpoon` Shows the harpoon dialog

`GotoHarpoon[1-5]` Goes to file saved in specific index

`SetHarpoon[1-5]` Sets current file to specific index

`AddToHarpoon` Sets the current file to the first available empty index

`ShowHarpoon` Shows harpoon's current file list, this list is editable and will update the indexs if moved around

### Example keybindings
 
By default, there are no hotkeys. I built this with the intention of using it alongside IdeaVim, so all my keybinds are done inside a ideavimrc File

 ### ideavimrc (`_ideavimrc`)

```vimrc
nmap <leader><C-h> :action SetHarpoon1<cr>
nmap <leader><C-t> :action SetHarpoon2<cr>
nmap <leader><C-n> :action SetHarpoon3<cr>
nmap <leader><C-s> :action SetHarpoon4<cr>

nmap <C-h> :action GotoHarpoon1<cr>
nmap <C-t> :action GotoHarpoon2<cr>
nmap <C-n> :action GotoHarpoon3<cr>
nmap <C-s> :action GotoHarpoon4<cr>

nmap <C-e> :action ShowHarpoon<cr>
nmap <C-a> :action AddToHarpoon<cr>
```